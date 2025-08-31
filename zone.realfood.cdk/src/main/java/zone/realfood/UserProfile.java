package zone.realfood;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.ITable;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableClass;
import software.amazon.awscdk.services.dynamodb.TableEncryption;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.ProjectionType;
import software.constructs.Construct;

/**
 * Stack that provides the DynamoDB table for storing Google-authenticated user profiles.
 *
 * Table name: UserProfile
 * - PK: userId (Google subject identifier or stable app user id)
 * - GSI: gsi_email (partition key: email) for lookup by email when needed
 */
public class UserProfile extends Stack {

    private final Table table;

    public UserProfile(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

    this.table = Table.Builder.create(this, "UserProfileTable")
        .tableName("UserProfile")
        .partitionKey(Attribute.builder().name("userId").type(AttributeType.STRING).build())
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .encryption(TableEncryption.AWS_MANAGED)
        .tableClass(TableClass.STANDARD)
        .removalPolicy(RemovalPolicy.RETAIN)
        .build();

        // Optional GSI to allow querying by email address
    this.table.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
        .indexName("gsi_email")
        .partitionKey(Attribute.builder().name("email").type(AttributeType.STRING).build())
        .projectionType(ProjectionType.ALL)
        .build());
    }

    public ITable getTable() {
        return this.table;
    }
}
