package zone.realfood;

import java.util.Map;
import java.util.List;
import java.time.Duration;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;

public class MainHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private static final DynamoDbTable<UserProfile> userProfileTable = initDynamoDbUserProfileTable();
  private static final TemplateEngine templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    String path = input.getPath() == null ? "/" : input.getPath();
    Map<String, String> query = input.getQueryStringParameters();
    Map<String, String> headers = input.getHeaders();

    if ("/".equals(path)) {
      IndexPage index = new IndexPage(userProfileTable, templateEngine, query, headers);
      return html(200, index.render());
    }
    if ("/login".equals(path)) {
      String existingSid = Cookies.getCookie(headers, "sid");
      if (existingSid != null && !existingSid.isBlank()) {
        return redirect("/");
      }
      LoginPage page = new LoginPage(templateEngine, query, headers);
      return html(200, page.render());
    }
    if ("/auth/google".equals(path)) {
      // Create state and set as cookie, then redirect to Google
      // Ensure env configuration exists
      if (GoogleOAuth.getClientId() == null || GoogleOAuth.getClientSecret() == null || GoogleOAuth.getRedirectUri() == null) {
        return html(500, "Google OAuth is not configured.");
      }
      String state = GoogleOAuth.randomState();
      String authorizeUrl = GoogleOAuth.buildAuthorizeUrl(state);
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
        GoogleOAuth.GoogleUser gu = GoogleOAuth.exchangeCodeForUser(code);
        // Persist or update user profile. Use sub as userId; minimal scopes include email
        String userId = "google:" + gu.sub();
        UserProfile existing = userProfileTable.getItem(r -> r.key(k -> k.partitionValue(userId)));
        if (existing == null) {
          existing = new UserProfile();
          existing.setUserId(userId);
        }
        existing.setEmail(gu.email());
        if (gu.name() != null)
          existing.setName(gu.name());
        if (gu.picture() != null)
          existing.setPictureUrl(gu.picture());
        existing.setScopes(List.of("openid", "email"));
        userProfileTable.putItem(existing);

        // Set session cookie and clear state cookie
        String sidCookie = Cookies.buildCookie("sid", userId, Duration.ofDays(30));
        String clearState = Cookies.buildCookie("g_state", "", Duration.ZERO);
        return redirect("/", sidCookie, clearState);
      } catch (Exception e) {
        return html(500, "Login failed: " + e.getMessage());
      }
    }
    if ("/logout".equals(path)) {
      String clearSid = Cookies.buildCookie("sid", "", Duration.ZERO);
      return redirect("/", clearSid);
    }

    return html(404, "Not found");
  }

  private static DynamoDbTable<UserProfile> initDynamoDbUserProfileTable() {
    DynamoDbClient client = DynamoDbClient.builder().region(Region.of(System.getenv().getOrDefault("AWS_REGION", System.getenv("AWS_DEFAULT_REGION"))))
        .credentialsProvider(DefaultCredentialsProvider.create()).build();
    DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    return enhancedClient.table(System.getenv("USER_PROFILE_TABLE"), TableSchema.fromBean(UserProfile.class));
  }

  private static APIGatewayProxyResponseEvent html(int status, String body) {
    return new APIGatewayProxyResponseEvent().withStatusCode(status).withHeaders(Map.of("Content-Type", "text/html; charset=utf-8")).withBody(body);
  }

  private static APIGatewayProxyResponseEvent redirect(String location, String... cookies) {
    APIGatewayProxyResponseEvent res = new APIGatewayProxyResponseEvent().withStatusCode(302).withHeaders(Map.of("Location", location));
    if (cookies != null && cookies.length > 0) {
      java.util.HashMap<String, java.util.List<String>> mv = new java.util.HashMap<>();
      mv.put("Set-Cookie", java.util.Arrays.asList(cookies));
      res.setMultiValueHeaders(mv);
    }
    return res;
  }
}
