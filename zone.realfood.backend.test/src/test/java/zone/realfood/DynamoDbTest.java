package zone.realfood;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import zone.realfood.db.DynamoDbTools;

public class DynamoDbTest {

    private static final String TABLE_NAME = DynamoDbTools.getUserProfileTableName() + "_Test";
    private static final String USER_ID = "tester01";
    private static final String USER_EMAIL = "tester01@cool.yo";
    private static final String USER_NAME = "Tester Testerson";
    private static final String USER_PIC_URL = "http://cool.yo/tester01.png";
    private static final List<String> USER_SCOPES = List.of("openid", "email");

    private DynamoDbClient client;
    private DynamoDbTable<UserProfile> table;

    @BeforeAll
    public static void setUpClass() {
        Fixtures.configureDynamoDbAccess();
        Assumptions.assumeTrue(Fixtures.isDynamoDbLocalRunning(), () -> "Local DynamoDB must be running.");
    }

    @BeforeEach
    public void setUp() {
        client = DynamoDbTools.createDynamoDbClient();
        Fixtures.createTable(client, TABLE_NAME);
        table = DynamoDbTools.createDynamoDbTable(client, TABLE_NAME, UserProfile.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        table.scan().items().forEach(item -> {
            try {
                table.deleteItem(item);
            } catch (Exception e) {
                Assertions.fail("Failed to delete item during teardown: " + e.getMessage());
            }
        });

        try {
            client.deleteTable(b -> b.tableName(TABLE_NAME));
            client.waiter().waitUntilTableNotExists(b -> b.tableName(TABLE_NAME));
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
}
