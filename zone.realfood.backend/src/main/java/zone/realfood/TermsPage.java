package zone.realfood;

import java.util.Map;

import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

public class TermsPage extends BasePage {
    public TermsPage(TemplateEngine templateEngine, Map<String, String> queryParams, Map<String, String> headers) {
        super(templateEngine, queryParams, headers);
    }

    @Override
    public String render() {
        java.util.HashMap<String, Object> params = new java.util.HashMap<>();
        params.put("title", "Terms of Service");
        params.put("staticBaseUrl", staticBaseUrl);
        params.put("userEmail", null);
        params.put("csrfToken", null);
        StringOutput output = new StringOutput();
        templateEngine.render("terms.jte", params, output);
        return output.toString();
    }
}
