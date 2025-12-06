package zone.realfood;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import zone.realfood.db.DynamoDbTools;

public class MainHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final TemplateEngine templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);

    private final DynamoDbTable<UserProfile> userProfileTable;
    private final GoogleOAuthService oauthService;

    public MainHandler() {
        this(DynamoDbTools.initDynamoDbUserProfileTable(), new GoogleOAuthService());
    }

    public MainHandler(DynamoDbTable<UserProfile> userProfileTable) {
        this(userProfileTable, new GoogleOAuthService());
    }

    public MainHandler(DynamoDbTable<UserProfile> userProfileTable, GoogleOAuthService oauthService) {
        this.userProfileTable = userProfileTable;
        this.oauthService = oauthService;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        if (input == null) {
            return html(400, "Bad request");
        }

        String path = input.getRawPath() == null ? "/" : input.getRawPath();
        String method = input.getRequestContext() != null && input.getRequestContext().getHttp() != null
                && input.getRequestContext().getHttp().getMethod() != null
                        ? input.getRequestContext().getHttp().getMethod().toUpperCase()
                        : "GET";
        Map<String, String> query = input.getQueryStringParameters();
        Map<String, String> headers = input.getHeaders() == null ? new HashMap<>() : new HashMap<>(input.getHeaders());

        List<String> cookies = input.getCookies();
        if ((cookies != null && !cookies.isEmpty()) && !headers.containsKey("Cookie")) {
            headers.put("Cookie", String.join("; ", cookies));
        }

        if ("/".equals(path)) {
            IndexPage index = new IndexPage(userProfileTable, templateEngine, query, headers);
            return html(200, index.render());
        }
        if ("/login".equals(path)) {
            if (SessionTools.getUserProfileFromSession(headers, userProfileTable) != null) {
                return redirect("/");
            }
            LoginPage page = new LoginPage(templateEngine, query, headers);
            return html(200, page.render());
        }
        if ("/privacy".equals(path)) {
            PrivacyPage page = new PrivacyPage(templateEngine, query, headers);
            return html(200, page.render());
        }
        if ("/terms".equals(path)) {
            TermsPage page = new TermsPage(templateEngine, query, headers);
            return html(200, page.render());
        }
        if ("/auth/google".equals(path)) {
            // Create state and set as cookie, then redirect to Google
            // Ensure env configuration exists
            if (GoogleOAuthService.getClientId() == null || GoogleOAuthService.getClientSecret() == null || GoogleOAuthService.getRedirectUri() == null) {
                return html(500, "Google OAuth is not configured.");
            }
            String state = GoogleOAuthService.randomState();
            String authorizeUrl = oauthService.buildAuthorizeUrl(state);
            String stateCookie = Cookies.buildCookie("g_state", state, Duration.ofMinutes(10));
            return redirect(authorizeUrl, stateCookie);
        }
        if ("/auth/google/callback".equals(path)) {
            String code = query != null ? query.get("code") : null;
            String state = query != null ? query.get("state") : null;
            String stateCookie = Cookies.getCookie(headers, "g_state");
            if (code == null || state == null || stateCookie == null || !state.equals(stateCookie)) {
                return html(400, "Invalid OAuth state");
            }
            try {
                GoogleOAuthService.GoogleUser gu = oauthService.exchangeCodeForUser(code);
                // Persist or update user profile. Use sub as userId; minimal scopes include email
                String userId = gu.sub();
                UserProfile existing = userProfileTable.getItem(r -> r.key(k -> k.partitionValue(userId)));
                if (existing == null) {
                    existing = new UserProfile();
                    existing.setUserId(userId);
                }
                existing.setEmail(gu.email());
                if (gu.name() != null) {
                    existing.setName(gu.name());
                }
                if (gu.picture() != null) {
                    existing.setPictureUrl(gu.picture());
                }
                existing.setScopes(List.of("openid", "email", "profile"));

                // Create new session on profile
                String sidCookie = SessionTools.createSession(existing, userProfileTable);
                String clearState = Cookies.buildCookie("g_state", "", Duration.ZERO);
                return redirect("/", sidCookie, clearState);
            } catch (Exception e) {
                return html(500, "Login failed: " + e.getMessage());
            }
        }
        if ("/logout".equals(path)) {
            UserProfile userProfile = SessionTools.getUserProfileFromSession(headers, userProfileTable);
            if ("POST".equals(method)) {
                if (userProfile == null) {
                    return redirect("/");
                }
                Map<String, String> formParams = parseForm(input.getBody(), Boolean.TRUE.equals(input.getIsBase64Encoded()));
                String csrfToken = formParams.get("csrfToken");
                String expected = userProfile.getCsrfToken();
                if (csrfToken == null || expected == null || !expected.equals(csrfToken)) {
                    return html(400, "Invalid CSRF token");
                }
                SessionTools.invalidateSession(headers, userProfileTable);
                String clearSid = Cookies.buildCookie("sid", "", Duration.ZERO);
                return redirect("/", clearSid);
            }
            if (userProfile == null) {
                return redirect("/login");
            }
            LogoutPage logoutPage = new LogoutPage(templateEngine, query, headers, userProfileTable, userProfile);
            return html(200, logoutPage.render());
        }

        return html(404, "Not found");
    }

    private static APIGatewayV2HTTPResponse html(int status, String body) {
        return APIGatewayV2HTTPResponse.builder().withStatusCode(status).withHeaders(Map.of("Content-Type", "text/html; charset=utf-8"))
                .withBody(body).build();
    }

    private static APIGatewayV2HTTPResponse redirect(String location, String... cookies) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Location", location);
        List<String> cookieList = cookies == null ? List.of() : Arrays.asList(cookies);
        return APIGatewayV2HTTPResponse.builder().withStatusCode(302).withHeaders(headers).withCookies(cookieList).withBody("")
                .build();
    }

    private static Map<String, String> parseForm(String body, boolean isBase64Encoded) {
        Map<String, String> params = new HashMap<>();
        if (body == null || body.isBlank()) {
            return params;
        }
        // HTTP API v2 base64-encodes non-JSON bodies (like form submissions)
        String decodedBody = isBase64Encoded 
                ? new String(java.util.Base64.getDecoder().decode(body), StandardCharsets.UTF_8)
                : body;
        for (String pair : decodedBody.split("&")) {
            int idx = pair.indexOf('=');
            String key;
            String value = "";
            if (idx >= 0) {
                key = pair.substring(0, idx);
                value = pair.substring(idx + 1);
            } else {
                key = pair;
            }
            try {
                key = java.net.URLDecoder.decode(key, StandardCharsets.UTF_8);
                value = java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
            params.put(key, value);
        }
        return params;
    }
}
