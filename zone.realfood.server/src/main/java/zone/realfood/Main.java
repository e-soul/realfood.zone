package zone.realfood;

import com.sun.net.httpserver.HttpServer;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

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
        try {
            client.describeTable(b -> b.tableName(DynamoDbTools.USER_PROFILE_TABLE_NAME_DEFAULT));
        } catch (ResourceNotFoundException ignore) {
            CreateTableRequest.Builder req = CreateTableRequest.builder().tableName(DynamoDbTools.USER_PROFILE_TABLE_NAME_DEFAULT)
                    .keySchema(KeySchemaElement.builder().attributeName("userId").keyType(KeyType.HASH).build())
                    .attributeDefinitions(AttributeDefinition.builder().attributeName("userId").attributeType(ScalarAttributeType.S).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST);
            client.createTable(req.build());
            client.waiter().waitUntilTableExists(DescribeTableRequest.builder().tableName(DynamoDbTools.USER_PROFILE_TABLE_NAME_DEFAULT).build());
        }

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
        DynamoDbTable<UserProfile> userProfileTable = enhancedClient.table(DynamoDbTools.USER_PROFILE_TABLE_NAME_DEFAULT, TableSchema.fromBean(UserProfile.class));
        TemplateEngine templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);

        HttpServer server = HttpServer.create(new InetSocketAddress(8050), 0);
        // Dynamic routes mirroring MainHandler
        server.createContext("/", exchange -> {
            try {
                URI requestUri = exchange.getRequestURI();
                String path = requestUri.getPath();
                if (path == null || path.isBlank()) path = "/";

                Map<String, String> query = parseQuery(requestUri);
                Map<String, String> headers = headersToSingleValueMap(exchange.getRequestHeaders());

                if ("/".equals(path)) {
                    IndexPage indexPage = new IndexPage(userProfileTable, templateEngine, query, headers);
                    writeHtml(exchange, 200, indexPage.render());
                    return;
                }
                if ("/login".equals(path)) {
                    String existingSid = Cookies.getCookie(headers, "sid");
                    if (existingSid != null && !existingSid.isBlank()) {
                        redirect(exchange, "/");
                        return;
                    }
                    LoginPage page = new LoginPage(templateEngine, query, headers);
                    writeHtml(exchange, 200, page.render());
                    return;
                }
                if ("/privacy".equals(path)) {
                    PrivacyPage page = new PrivacyPage(templateEngine, query, headers);
                    writeHtml(exchange, 200, page.render());
                    return;
                }
                if ("/terms".equals(path)) {
                    TermsPage page = new TermsPage(templateEngine, query, headers);
                    writeHtml(exchange, 200, page.render());
                    return;
                }
                if ("/auth/google".equals(path)) {
                    if (GoogleOAuth.getClientId() == null || GoogleOAuth.getClientSecret() == null || GoogleOAuth.getRedirectUri() == null) {
                        writeHtml(exchange, 500, "Google OAuth is not configured.");
                        return;
                    }
                    String state = GoogleOAuth.randomState();
                    String authorizeUrl = GoogleOAuth.buildAuthorizeUrl(state);
                    String stateCookie = Cookies.buildCookie("g_state", state, Duration.ofMinutes(10));
                    redirect(exchange, authorizeUrl, java.util.List.of(stateCookie));
                    return;
                }
                if ("/auth/google/callback".equals(path)) {
                    String code = query.get("code");
                    String state = query.get("state");
                    String stateCookieVal = Cookies.getCookie(headers, "g_state");
                    if (code == null || state == null || stateCookieVal == null || !state.equals(stateCookieVal)) {
                        writeHtml(exchange, 400, "Invalid OAuth state");
                        return;
                    }
                    try {
                        GoogleOAuth.GoogleUser gu = GoogleOAuth.exchangeCodeForUser(code);
                        String userId = "google:" + gu.sub();
                        UserProfile existing = userProfileTable.getItem(r -> r.key(k -> k.partitionValue(userId)));
                        if (existing == null) {
                            existing = new UserProfile();
                            existing.setUserId(userId);
                        }
                        existing.setEmail(gu.email());
                        if (gu.name() != null) existing.setName(gu.name());
                        if (gu.picture() != null) existing.setPictureUrl(gu.picture());
                        existing.setScopes(java.util.List.of("openid", "email"));
                        userProfileTable.putItem(existing);

                        String sidCookie = Cookies.buildCookie("sid", userId, Duration.ofDays(30));
                        String clearState = Cookies.buildCookie("g_state", "", Duration.ZERO);
                        redirect(exchange, "/", java.util.List.of(sidCookie, clearState));
                        return;
                    } catch (Exception e) {
                        writeHtml(exchange, 500, "Login failed: " + e.getMessage());
                        return;
                    }
                }
                if ("/logout".equals(path)) {
                    String clearSid = Cookies.buildCookie("sid", "", Duration.ZERO);
                    redirect(exchange, "/", java.util.List.of(clearSid));
                    return;
                }

                writeHtml(exchange, 404, "Not found");
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

    private static void redirect(com.sun.net.httpserver.HttpExchange exchange, String location) throws java.io.IOException {
        redirect(exchange, location, java.util.List.of());
    }

    private static void redirect(com.sun.net.httpserver.HttpExchange exchange, String location, java.util.List<String> setCookies) throws java.io.IOException {
        exchange.getResponseHeaders().set("Location", location);
        if (setCookies != null) {
            for (String c : setCookies) {
                if (c != null && !c.isBlank()) {
                    exchange.getResponseHeaders().add("Set-Cookie", c);
                }
            }
        }
        exchange.sendResponseHeaders(302, -1);
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

    private static Map<String, String> headersToSingleValueMap(com.sun.net.httpserver.Headers headers) {
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (!e.getValue().isEmpty()) {
                String first = e.getValue().get(0);
                String key = e.getKey();
                out.put(key, first);
                if (key != null) {
                    out.put(key.toLowerCase(Locale.ROOT), first);
                }
            }
        }
        return out;
    }
}
