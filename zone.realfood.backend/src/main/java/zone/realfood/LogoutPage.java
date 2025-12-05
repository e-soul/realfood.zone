package zone.realfood;

import java.util.HashMap;
import java.util.Map;

import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

public class LogoutPage extends BasePage {

    private final DynamoDbTable<UserProfile> userProfileTable;
    private final UserProfile userProfile;

    public LogoutPage(TemplateEngine templateEngine, Map<String, String> queryParams, Map<String, String> headers,
            DynamoDbTable<UserProfile> userProfileTable, UserProfile userProfile) {
        super(templateEngine, queryParams, headers);
        this.userProfileTable = userProfileTable;
        this.userProfile = userProfile;
    }

    @Override
    public String render() {
        if (userProfile == null) {
            return "";
        }
        String csrfToken = SessionTools.ensureCsrfToken(userProfile, userProfileTable);

        Map<String, Object> params = new HashMap<>();
        params.put("title", "Confirm Logout");
        params.put("staticBaseUrl", staticBaseUrl);
        params.put("userEmail", userProfile.getEmail());
        params.put("csrfToken", csrfToken);
        StringOutput output = new StringOutput();
        templateEngine.render("logout.jte", params, output);
        return output.toString();
    }
}
