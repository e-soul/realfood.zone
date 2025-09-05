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
        String email = Cookies.getCookie(headers, "sid");
        LayoutModel layout = new LayoutModel("Terms of Service", staticBaseUrl, cssUrl, email);
        StringOutput output = new StringOutput();
        templateEngine.render("terms.jte", layout, output);
        return output.toString();
    }
}
