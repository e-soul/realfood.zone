package zone.realfood;

import java.util.Map;

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

    IndexPage index = new IndexPage(userProfileTable, templateEngine, input.getQueryStringParameters());

    return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(Map.of("Content-Type", "text/html; charset=utf-8")).withBody(index.render());
  }

  private static DynamoDbTable<UserProfile> initDynamoDbUserProfileTable() {
    DynamoDbClient client = DynamoDbClient.builder().region(Region.of(System.getenv().getOrDefault("AWS_REGION", System.getenv("AWS_DEFAULT_REGION"))))
        .credentialsProvider(DefaultCredentialsProvider.create()).build();
    DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    return enhancedClient.table(System.getenv("USER_PROFILE_TABLE"), TableSchema.fromBean(UserProfile.class));
  }
}
