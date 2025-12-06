package zone.realfood;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.sun.net.httpserver.HttpServer;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
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

        Fixtures.configureDynamoDbAccess();

        DynamoDbClient client = DynamoDbTools.createDynamoDbClient();
        String tableName = DynamoDbTools.getUserProfileTableName();

        Fixtures.createTable(client, tableName);

        DynamoDbTable<UserProfile> userProfileTable = DynamoDbTools.createDynamoDbTable(client, tableName, UserProfile.class);
        userProfileTable.scan().items().forEach(item -> {
            try {
                userProfileTable.deleteItem(item);
                System.out.println("Deleted: " + item);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete item", e);
            }
        });
        MainHandler handler = new MainHandler(userProfileTable);

        HttpServer server = HttpServer.create(new InetSocketAddress(8050), 0);
        // Dynamic routes mirroring MainHandler
        server.createContext("/", exchange -> {
            try {
                URI requestUri = exchange.getRequestURI();
                String path = requestUri.getPath();
                if (path == null || path.isBlank()) {
                    path = "/";
                }
                String method = exchange.getRequestMethod();

                Map<String, String> query = parseQuery(requestUri);
                Map<String, List<String>> headers = exchange.getRequestHeaders();

                System.out.println();
                System.out.println("Request: " + path);
                query.forEach((k, v) -> System.out.println("Query: " + k + "=" + v));
                headers.forEach((k, v) -> System.out.println("Header: " + k + "=" + v));

                byte[] requestBodyBytes = exchange.getRequestBody().readAllBytes();
                String requestBody = requestBodyBytes.length == 0 ? null : new String(requestBodyBytes, StandardCharsets.UTF_8);
                System.out.println("Body: " + requestBody);

                APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent().withHttpMethod(method).withBody(requestBody)
                        .withMultiValueHeaders(headers).withPath(path).withQueryStringParameters(query);

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
        if (raw == null || raw.isBlank()) {
            return map;
        }

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
            } catch (Exception ignored) {
            }
        }
        return map;
    }
}
