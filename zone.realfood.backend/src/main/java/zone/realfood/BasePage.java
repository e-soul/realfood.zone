package zone.realfood;

import java.util.Map;

import gg.jte.TemplateEngine;

public abstract class BasePage implements Page {

    final TemplateEngine templateEngine;
    final Map<String, String> queryParams;
    final Map<String, String> headers;
    final String staticBaseUrl;
    final String cssUrl;

    public BasePage(TemplateEngine templateEngine, Map<String, String> queryParams, Map<String, String> headers) {
        this.templateEngine = templateEngine;
        this.queryParams = queryParams;
        this.headers = headers;
        staticBaseUrl = System.getenv("STATIC_BASE_URL");
        cssUrl = staticBaseUrl != null && !staticBaseUrl.isBlank()
            ? staticBaseUrl + "/styles/style.css"
            : null;
    }
}
