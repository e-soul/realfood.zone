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
        String name = queryParams != null ? queryParams.getOrDefault("name", "world") : "world";

        String sessionUserId = Cookies.getCookie(headers, "sid");
        UserProfile profile = null;
        String error = null;
        String userId = null;
        try {
            if (sessionUserId != null && !sessionUserId.isBlank()) {
                final String uid = sessionUserId;
                profile = userProfileTable.getItem(r -> r.key(k -> k.partitionValue(uid)));
                userId = uid;
            }
        } catch (Exception e) {
            error = e.getMessage();
        }

    String email = profile != null ? profile.getEmail() : null;
    StringOutput output = new StringOutput();
    java.util.HashMap<String, Object> params = new java.util.HashMap<>();
    params.put("title", "Hello");
    params.put("greeting", "Hello");
    params.put("name", name);
    params.put("cssUrl", cssUrl);
    params.put("profile", email == null ? "" : email);
    params.put("userId", userId == null ? "" : userId);
    params.put("error", error == null ? "" : error);
    templateEngine.render("index.jte", params, output);
        return output.toString();
    }
}
