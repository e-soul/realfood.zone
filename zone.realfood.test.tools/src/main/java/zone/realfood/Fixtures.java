package zone.realfood;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

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

public final class Fixtures {

    private Fixtures() {
        // no instances
    }

    public static void configureDynamoDbAccess() {
        System.setProperty("aws.accessKeyId", "dummyAccessKeyIdForLocalDynamoDb");
        System.setProperty("aws.secretAccessKey", "dummySecretAccessKeyForLocalDynamoDb");
        System.setProperty(DynamoDbTools.DYNAMODB_ENDPOINT_SYS_PROP, "http://localhost:5050");
    }

    public static boolean isDynamoDbLocalRunning() {
        URI endpoint = URI.create(System.getProperty(DynamoDbTools.DYNAMODB_ENDPOINT_SYS_PROP));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void createTable(DynamoDbClient client, String tableName) {
        try {
            client.describeTable(b -> b.tableName(tableName));
            System.out.println("Table " + tableName + " already exists.");
        } catch (ResourceNotFoundException ignore) {
            CreateTableRequest.Builder req = CreateTableRequest.builder().tableName(tableName)
                    .keySchema(KeySchemaElement.builder().attributeName("userId").keyType(KeyType.HASH).build())
                    .attributeDefinitions(AttributeDefinition.builder().attributeName("userId").attributeType(ScalarAttributeType.S).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST);
            client.createTable(req.build());
            client.waiter().waitUntilTableExists(DescribeTableRequest.builder().tableName(tableName).build());
            System.out.println("Table " + tableName + " created.");
        }
    }
}
