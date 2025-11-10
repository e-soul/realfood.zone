package zone.realfood;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sun.net.httpserver.HttpServer;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import zone.realfood.db.DynamoDbTools;

public class Main {
    
    public static void main(String[] args) throws Exception {
        String staticContentDirStr = System.getProperty("zone.realfood.staticContentDir");
        if (staticContentDirStr == null) {
            System.out.println("Set -Dzone.realfood.staticContentDir=path/to/static-content");
            return;
        }
        Path staticContentDir = Path.of(staticContentDirStr);
        if (Files.notExists(staticContentDir)) {
            System.out.println("Static content directory does not exist: " + staticContentDir);
            return;
        }

        System.setProperty("aws.accessKeyId", "dummyAccessKeyIdForLocalDynamoDb");
        System.setProperty("aws.secretAccessKey", "dummySecretAccessKeyForLocalDynamoDb");
        System.setProperty(DynamoDbTools.DYNAMODB_ENDPOINT_SYS_PROP, "http://localhost:5050");

        DynamoDbClient client = DynamoDbTools.createDynamoDbClient();
        String tableName = DynamoDbTools.getUserProfileTableName();

        createTable(client, tableName);

        DynamoDbTable<UserProfile> userProfileTable = DynamoDbTools.createDynamoDbTable(client, tableName, UserProfile.class);
        MainHandler handler = new MainHandler(userProfileTable);

        HttpServer server = HttpServer.create(new InetSocketAddress(8050), 0);
        // Dynamic routes mirroring MainHandler
        server.createContext("/", exchange -> {
            try {
                URI requestUri = exchange.getRequestURI();
                String path = requestUri.getPath();
                if (path == null || path.isBlank()) path = "/";

                Map<String, String> query = parseQuery(requestUri);
                Map<String, List<String>> headers = exchange.getRequestHeaders();

                APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent().withMultiValueHeaders(headers).withPath(path).withQueryStringParameters(query);

                APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(requestEvent, null);
                
                exchange.getResponseHeaders().putAll(responseEvent.getMultiValueHeaders());
                byte[] body = responseEvent.getBody().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(responseEvent.getStatusCode(), body.length);
                if (body.length > 0) {
                    exchange.getResponseBody().write(body);
                }
                exchange.close();
            } catch (Exception e) {
                writeHtml(exchange, 500, "Server error: " + e.getMessage());
            }
        });

        // Test OAuth endpoints for unit tests to avoid external Google calls
        // Token endpoint: expects form-encoded body with code=XYZ; returns minimal JSON with id_token & access_token
        server.createContext("/__test/google-oauth/token", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeHtml(exchange, 405, "Method Not Allowed");
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            // naive parse
            String code = null;
            for (String part : body.split("&")) {
                int idx = part.indexOf('=');
                if (idx > 0) {
                    String k = java.net.URLDecoder.decode(part.substring(0, idx), java.nio.charset.StandardCharsets.UTF_8);
                    String v = java.net.URLDecoder.decode(part.substring(idx + 1), java.nio.charset.StandardCharsets.UTF_8);
                    if ("code".equals(k)) code = v;
                }
            }
            if (code == null || code.isBlank()) {
                String json = "{\"error\":\"invalid_code\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, json.getBytes().length);
                exchange.getResponseBody().write(json.getBytes());
                exchange.close();
                return;
            }
            // Build predictable tokens from code for assertions
            String idToken = "idtoken-" + code;
            String accessToken = "accesstoken-" + code;
            String json = "{\"id_token\":\"" + idToken + "\",\"access_token\":\"" + accessToken + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });

        server.createContext("/__test/google-oauth/userinfo", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeHtml(exchange, 405, "Method Not Allowed");
                return;
            }
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth == null || !auth.startsWith("Bearer accesstoken-")) {
                String json = "{\"error\":\"invalid_token\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(401, json.getBytes().length);
                exchange.getResponseBody().write(json.getBytes());
                exchange.close();
                return;
            }
            String accessToken = auth.substring("Bearer ".length());
            String code = accessToken.replaceFirst("accesstoken-", "");
            // Provide deterministic user info using code
            String sub = "sub-" + code;
            String email = code + "@example.test";
            String name = "Test User " + code;
            String picture = "http://localhost/pic-" + code + ".png";
            String json = "{\"sub\":\"" + sub + "\",\"email\":\"" + email + "\",\"email_verified\":true,\"name\":\"" + name + "\",\"picture\":\"" + picture + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });

        server.createContext("/static-content/", exchange -> {
            URI requestUri = exchange.getRequestURI();
            String path = requestUri.getPath();
            path = path.replaceFirst("/static-content/", "");
            Path filePath = staticContentDir.resolve(path).normalize();
            if (Files.notExists(filePath) || Files.isDirectory(filePath)) {
                String response = "404 (Not Found)\n";
                exchange.sendResponseHeaders(404, response.getBytes().length);
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                return;
            }
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            exchange.getResponseHeaders().set("Content-Type", contentType);
            byte[] fileBytes = Files.readAllBytes(filePath);
            exchange.sendResponseHeaders(200, fileBytes.length);
            exchange.getResponseBody().write(fileBytes);
            exchange.close();
        });

        server.start();
        System.out.println("Server started on http://localhost:8050");
        Thread.sleep(180 * 1000);
    }

    private static void createTable(DynamoDbClient client, String tableName) {
        try {
            client.describeTable(b -> b.tableName(tableName));
        } catch (ResourceNotFoundException ignore) {
            CreateTableRequest.Builder req = CreateTableRequest.builder().tableName(tableName)
                    .keySchema(KeySchemaElement.builder().attributeName("userId").keyType(KeyType.HASH).build())
                    .attributeDefinitions(AttributeDefinition.builder().attributeName("userId").attributeType(ScalarAttributeType.S).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST);
            client.createTable(req.build());
            client.waiter().waitUntilTableExists(DescribeTableRequest.builder().tableName(tableName).build());
        }
    }

    private static void writeHtml(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> map = new HashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null || raw.isBlank()) return map;
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            try {
                if (idx >= 0) {
                    String k = java.net.URLDecoder.decode(pair.substring(0, idx), java.nio.charset.StandardCharsets.UTF_8);
                    String v = java.net.URLDecoder.decode(pair.substring(idx + 1), java.nio.charset.StandardCharsets.UTF_8);
                    map.put(k, v);
                } else {
                    String k = java.net.URLDecoder.decode(pair, java.nio.charset.StandardCharsets.UTF_8);
                    map.put(k, "");
                }
            } catch (Exception ignored) {}
        }
        return map;
    }
}
