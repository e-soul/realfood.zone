package zone.realfood;

import java.util.Map;

import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

public class IndexPage extends BasePage {

    private final DynamoDbTable<UserProfile> userProfileTable;

    public IndexPage(DynamoDbTable<UserProfile> userProfileTable, TemplateEngine templateEngine, Map<String, String> queryParams) {
        super(templateEngine, queryParams);
        this.userProfileTable = userProfileTable;
    }

    public String render() {
        String name = queryParams.getOrDefault("name", "world");
        String userId = queryParams.getOrDefault("userId", "demo-user");
        UserProfile profile = userProfileTable.getItem(r -> r.key(k -> k.partitionValue(userId)));
        IndexModel model = new IndexModel("Hello, " + name + "!", "Hello", name, cssUrl, profile.getEmail(), userId, "error");
        StringOutput output = new StringOutput();
        templateEngine.render("index.jte", model, output);
        return output.toString();
    }
}
