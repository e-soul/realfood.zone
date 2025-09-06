package zone.realfood;

import java.util.Map;

import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

public class LoginPage extends BasePage {
    public LoginPage(TemplateEngine templateEngine, Map<String, String> queryParams, Map<String, String> headers) {
        super(templateEngine, queryParams, headers);
    }

    @Override
    public String render() {
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("title", "Login");
        params.put("staticBaseUrl", staticBaseUrl);
        params.put("userEmail", null);
        StringOutput output = new StringOutput();
        templateEngine.render("login.jte", params, output);
        return output.toString();
    }
}
