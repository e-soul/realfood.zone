package zone.realfood;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import zone.realfood.db.DynamoDbTools;

public class MainHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String path = input.getPath() == null ? "/" : input.getPath();
        Map<String, String> query = input.getQueryStringParameters();
        Map<String, String> headers = new HashMap<>();
        Map<String, List<String>> multiValueHeaders = input.getMultiValueHeaders();
        if (multiValueHeaders != null) {
            for (Map.Entry<String, List<String>> entry : multiValueHeaders.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    headers.put(entry.getKey(), entry.getValue().get(0));
                }
            }
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
            SessionTools.invalidateSession(headers, userProfileTable);
            String clearSid = Cookies.buildCookie("sid", "", Duration.ZERO);
            return redirect("/", clearSid);
        }

        return html(404, "Not found");
    }

    private static APIGatewayProxyResponseEvent html(int status, String body) {
        return new APIGatewayProxyResponseEvent().withStatusCode(status).withMultiValueHeaders(Map.of("Content-Type", List.of("text/html; charset=utf-8")))
                .withBody(body);
    }

    private static APIGatewayProxyResponseEvent redirect(String location, String... cookies) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Location", List.of(location));
        if (cookies != null && cookies.length > 0) {
            headers.put("Set-Cookie", Arrays.asList(cookies));
        }
        return new APIGatewayProxyResponseEvent().withStatusCode(302).withMultiValueHeaders(headers).withBody("");
    }
}
