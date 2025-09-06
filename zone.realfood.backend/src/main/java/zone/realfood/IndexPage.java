package zone.realfood;

import java.util.HashMap;
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
        UserProfile userProfile = null;
        try {
            if (sessionUserId != null && !sessionUserId.isBlank()) {
                userProfile = userProfileTable.getItem(r -> r.key(k -> k.partitionValue(sessionUserId)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("title", "Hello");
        params.put("userProfile", userProfile);
        params.put("userEmail", userProfile != null ? userProfile.getEmail() : null);
        params.put("staticBaseUrl", staticBaseUrl);
        StringOutput output = new StringOutput();
        templateEngine.render("index.jte", params, output);
        return output.toString();
    }
}
