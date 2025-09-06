package zone.realfood;

import java.util.Map;

import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

public class IndexPage extends BasePage {

    private final DynamoDbTable<UserProfile> userProfileTable;

    public IndexPage(DynamoDbTable<UserProfile> userProfileTable, TemplateEngine templateEngine, Map<String, String> queryParams, Map<String, String> headers) {
        super(templateEngine, queryParams, headers);
        this.userProfileTable = userProfileTable;
    }

    public String render() {
        String sessionUserId = Cookies.getCookie(headers, "sid");
        UserProfile profile = null;
        try {
            if (sessionUserId != null && !sessionUserId.isBlank()) {
                profile = userProfileTable.getItem(r -> r.key(k -> k.partitionValue(sessionUserId)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String email = profile != null ? profile.getEmail() : null;
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("title", "Hello");
        params.put("userEmail", email);
        params.put("staticBaseUrl", staticBaseUrl);
        StringOutput output = new StringOutput();
        templateEngine.render("index.jte", params, output);
        return output.toString();
    }
}
