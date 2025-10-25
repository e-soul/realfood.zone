package zone.realfood;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.util.List;

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
import zone.realfood.db.DynamoDbTools;

public class DynamoDbTest {

    private static final String USER_ID = "tester01";
    private static final String USER_EMAIL = "tester01@cool.yo";
    private static final String USER_NAME = "Tester Testerson";
    private static final String USER_PIC_URL = "http://cool.yo/tester01.png";
    private static final List<String> USER_SCOPES = List.of("openid", "email");

    private DynamoDbClient client;
    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<UserProfile> table;

    @BeforeAll
    public static void setUpClass() {
        System.setProperty("aws.accessKeyId", "dummyAccessKeyIdForLocalDynamoDb");
        System.setProperty("aws.secretAccessKey", "dummySecretAccessKeyForLocalDynamoDb");
        System.setProperty(DynamoDbTools.DYNAMODB_ENDPOINT_SYS_PROP, "http://localhost:5050");

        Assumptions.assumeTrue(Fixtures.isDynamoDbLocalRunning(), () -> "Local DynamoDB must be running.");
    }

    @BeforeEach
    public void setUp() {
        client = DynamoDbTools.createDynamoDbClient();
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
        table = createOrGetTable();
        Assertions.assertNotNull(table);
    }

    @AfterEach
    void tearDown() {
        table.scan().items().forEach(item -> {
            try {
                table.deleteItem(item);
            } catch (Exception e) {
                // ignore
            }
        });

        try {
            client.deleteTable(b -> b.tableName(DynamoDbTools.USER_PROFILE_TABLE_NAME_DEFAULT));
            client.waiter().waitUntilTableNotExists(b -> b.tableName(DynamoDbTools.USER_PROFILE_TABLE_NAME_DEFAULT));
        } catch (Exception e) {
            // ignore
        } finally {
            client.close();
        }
    }

    @Test
    public void testDb() {
        UserProfile existing = table.getItem(r -> r.key(k -> k.partitionValue(USER_ID)));
        Assertions.assertNull(existing);

        UserProfile user = new UserProfile();
        user.setUserId(USER_ID);
        user.setEmail(USER_EMAIL);
        user.setName(USER_NAME);
        user.setPictureUrl(USER_PIC_URL);
        user.setScopes(USER_SCOPES);
        table.putItem(user);

        existing = table.getItem(r -> r.key(k -> k.partitionValue(USER_ID)));

        Assertions.assertNotNull(existing);
        Assertions.assertEquals(USER_ID, existing.getUserId());
        Assertions.assertEquals(USER_EMAIL, existing.getEmail());
        Assertions.assertEquals(USER_NAME, existing.getName());
        Assertions.assertEquals(USER_PIC_URL, existing.getPictureUrl());
        Assertions.assertEquals(USER_SCOPES, existing.getScopes());
    }

    private DynamoDbTable<UserProfile> createOrGetTable() {
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
        return enhancedClient.table(DynamoDbTools.USER_PROFILE_TABLE_NAME_DEFAULT, TableSchema.fromBean(UserProfile.class));
    }
}
