package zone.realfood;

import java.util.Map;

import gg.jte.TemplateEngine;

public abstract class BasePage implements Page {

    protected final TemplateEngine templateEngine;
    protected final Map<String, String> queryParams;
    protected final Map<String, String> headers;
    protected final String staticBaseUrl;

    public BasePage(TemplateEngine templateEngine, Map<String, String> queryParams, Map<String, String> headers) {
        this.templateEngine = templateEngine;
        this.queryParams = queryParams;
        this.headers = headers;
        staticBaseUrl = System.getenv("STATIC_BASE_URL");
    }
}
