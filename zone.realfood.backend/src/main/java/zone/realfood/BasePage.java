package zone.realfood;

import java.util.Map;

import gg.jte.TemplateEngine;

public abstract class BasePage implements Page {

    final TemplateEngine templateEngine;
    final Map<String, String> queryParams;
    final String cssUrl;

    public BasePage(TemplateEngine templateEngine, Map<String, String> queryParams) {
        this.templateEngine = templateEngine;
        this.queryParams = queryParams;
        cssUrl = System.getenv("CSS_URL");
    }
}
