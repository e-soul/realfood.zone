package zone.realfood;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

public class MainHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private static final String TABLE_ENV = "USER_PROFILE_TABLE";
  private static final DynamoDbClient ddb = DynamoDbClient.builder()
      .region(Region.of(System.getenv().getOrDefault("AWS_REGION", System.getenv("AWS_DEFAULT_REGION"))))
      .credentialsProvider(DefaultCredentialsProvider.create()).build();
  private static final DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder().dynamoDbClient(ddb).build();
  private static DynamoDbTable<UserProfile> table;
  private static volatile boolean seeded = false;
  private static final TemplateEngine templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    String name = Optional.ofNullable(input).map(APIGatewayProxyRequestEvent::getQueryStringParameters).map(q -> q.getOrDefault("name", "world"))
        .orElse("world");

  String cssUrl = System.getenv("CSS_URL");

    String tableName = System.getenv(TABLE_ENV);
    if (table == null && tableName != null && !tableName.isBlank()) {
      table = enhanced.table(tableName, TableSchema.fromBean(UserProfile.class));
    }

    if (!seeded && table != null) {
      seedSampleData();
      seeded = true; // best-effort; multiple inits are fine
    }

    String userId = Optional.ofNullable(input).map(APIGatewayProxyRequestEvent::getQueryStringParameters).map(q -> q.get("userId")).orElse("demo-user");
    UserProfile profile = null;
    String error = null;
    if (table == null) {
      error = "No table configured.";
    } else {
      try {
        profile = table.getItem(r -> r.key(k -> k.partitionValue(userId)));
      } catch (Exception e) {
        error = e.getMessage();
      }
    }

    String body;
    try {
      StringOutput output = new StringOutput();
      templateEngine.render("index.jte", Map.of(
          "title", "Hello, " + name + "!",
          "greeting", "Hello",
          "name", name,
          "cssUrl", cssUrl,
          "profile", profile,
          "userId", userId,
          "error", error
      ), output);
      body = output.toString();
    } catch (Exception e) {
      body = "<p>Template error: " + escape(e.getMessage()) + "</p>";
    }

    return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(Map.of("Content-Type", "text/html; charset=utf-8")).withBody(body);
  }

  private void seedSampleData() {
    try {
      // Write 2 sample items if table seems empty (scan first page only)
      PageIterable<UserProfile> pages = table.scan();
      boolean hasAny = pages.stream().limit(1).anyMatch(p -> !p.items().isEmpty());
      if (hasAny)
        return;

      UserProfile u1 = new UserProfile();
      u1.setUserId("demo-user");
      u1.setEmail("demo@example.com");
      u1.setName("Demo User");
      u1.setPictureUrl("https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y");
      u1.setScopes(List.of("openid", "email", "profile"));

      UserProfile u2 = new UserProfile();
      u2.setUserId("alice-123");
      u2.setEmail("alice@example.com");
      u2.setName("Alice Wonderland");
      u2.setPictureUrl("https://www.gravatar.com/avatar/ffffffffffffffffffffffffffffffff?d=identicon");
      u2.setScopes(List.of("openid", "email"));

      table.putItem(u1);
      table.putItem(u2);
    } catch (Exception e) {
      // best-effort; ignore failures in seeding
    }
  }

  // renderProfile no longer used; rendering happens in the template

  private static String escape(String s) {
    if (s == null)
      return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
  }
}
