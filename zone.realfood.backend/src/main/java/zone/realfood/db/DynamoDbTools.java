package zone.realfood.db;

import java.net.URI;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import zone.realfood.UserProfile;

public final class DynamoDbTools {

    public static final String DYNAMODB_ENDPOINT_SYS_PROP = "zone.realfood.dynamodb.endpoint";
    public static final String USER_PROFILE_TABLE_NAME_ENV_VAR = "USER_PROFILE_TABLE";
    public static final String USER_PROFILE_TABLE_NAME_DEFAULT = "UserProfile";

    public static DynamoDbTable<UserProfile> initDynamoDbUserProfileTable() {
        DynamoDbClient client = createDynamoDbClient();
        String tableName = getUserProfileTableName();
        return createDynamoDbTable(client, tableName, UserProfile.class);
    }

    public static DynamoDbClient createDynamoDbClient() {
        DynamoDbClientBuilder clientBuilder = DynamoDbClient.builder();
        String endpointOverride = System.getProperty(DYNAMODB_ENDPOINT_SYS_PROP);
        if (null != endpointOverride) {
            clientBuilder = clientBuilder.endpointOverride(URI.create(endpointOverride));
        }
        return clientBuilder.region(getRegion()).credentialsProvider(DefaultCredentialsProvider.builder().build()).build();
    }

    public static String getUserProfileTableName() {
        return System.getenv().getOrDefault(USER_PROFILE_TABLE_NAME_ENV_VAR, USER_PROFILE_TABLE_NAME_DEFAULT);
    }

    public static <T> DynamoDbTable<T> createDynamoDbTable(DynamoDbClient client, String tableName, Class<T> tableClass) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
        return enhancedClient.table(tableName, TableSchema.fromBean(tableClass));
    }

    private static Region getRegion() {
        String region = System.getenv("AWS_REGION");
        if (null != region) {
            return Region.of(region);
        }
        return Region.EU_CENTRAL_1;
    }
}
