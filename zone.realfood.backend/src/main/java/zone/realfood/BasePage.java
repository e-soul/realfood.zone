package zone.realfood;

import java.util.Map;

import gg.jte.TemplateEngine;

public abstract class BasePage implements Page {

    final TemplateEngine templateEngine;
    final Map<String, String> queryParams;
    final Map<String, String> headers;
    final String cssUrl;

    public BasePage(TemplateEngine templateEngine, Map<String, String> queryParams, Map<String, String> headers) {
        this.templateEngine = templateEngine;
        this.queryParams = queryParams;
        this.headers = headers;
        cssUrl = System.getenv("CSS_URL");
    }
}
