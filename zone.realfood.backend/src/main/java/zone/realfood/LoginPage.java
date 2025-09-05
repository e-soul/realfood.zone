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
    // Generate a CSRF state and set cookie via handler; here just render link to /auth/google
    StringOutput output = new StringOutput();
    java.util.HashMap<String, Object> params = new java.util.HashMap<>();
    params.put("title", "Login");
    params.put("cssUrl", cssUrl);
    params.put("userEmail", null);
    templateEngine.render("login.jte", params, output);
        return output.toString();
    }
}
