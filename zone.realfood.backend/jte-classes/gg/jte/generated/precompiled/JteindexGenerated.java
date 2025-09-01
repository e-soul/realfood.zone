package gg.jte.generated.precompiled;
import zone.realfood.UserProfile;
@SuppressWarnings("unchecked")
public final class JteindexGenerated {
	public static final String JTE_NAME = "index.jte";
	public static final int[] JTE_LINE_INFO = {0,0,2,2,2,2,14,14,14,15,15,15,15,15,15,15,15,15,16,16,17,17,17,20,20,20,20,20,20,23,23,24,24,24,25,25,26,26,26,27,27,28,28,29,29,29,29,29,29,29,29,29,30,30,31,31,31,32,32,32,34,34,36,36,37,37,38,38,38,38,38,38,39,39,40,40,42,42,46,46,46,2,3,4,5,6,7,8,8,8,8};
	public static void render(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, String title, String greeting, String name, String cssUrl, UserProfile profile, String userId, String error) {
		jteOutput.writeContent("\r\n<!doctype html>\r\n<html lang=\"en\">\r\n  <head>\r\n    <meta charset=\"utf-8\">\r\n    ");
		if (cssUrl != null && !cssUrl.isBlank()) {
			jteOutput.writeContent("\r\n      <link rel=\"stylesheet\"");
			var __jte_html_attribute_0 = cssUrl;
			if (gg.jte.runtime.TemplateUtils.isAttributeRendered(__jte_html_attribute_0)) {
				jteOutput.writeContent(" href=\"");
				jteOutput.setContext("link", "href");
				jteOutput.writeUserContent(__jte_html_attribute_0);
				jteOutput.setContext("link", null);
				jteOutput.writeContent("\"");
			}
			jteOutput.writeContent(">\r\n    ");
		}
		jteOutput.writeContent("\r\n    <title>");
		jteOutput.setContext("title", null);
		jteOutput.writeUserContent(title);
		jteOutput.writeContent("</title>\r\n  </head>\r\n  <body>\r\n    <h3>");
		jteOutput.setContext("h3", null);
		jteOutput.writeUserContent(greeting);
		jteOutput.writeContent(", ");
		jteOutput.setContext("h3", null);
		jteOutput.writeUserContent(name);
		jteOutput.writeContent("!</h3>\r\n    <section>\r\n      <h4>UserProfile sample</h4>\r\n      ");
		if (error != null && !error.isBlank()) {
			jteOutput.writeContent("\r\n        <p>Error reading from DynamoDB: ");
			jteOutput.setContext("p", null);
			jteOutput.writeUserContent(error);
			jteOutput.writeContent("</p>\r\n      ");
		} else if (profile == null) {
			jteOutput.writeContent("\r\n        <p>No profile found for userId '<code>");
			jteOutput.setContext("code", null);
			jteOutput.writeUserContent(userId);
			jteOutput.writeContent("</code>'.</p>\r\n      ");
		} else {
			jteOutput.writeContent("\r\n        ");
			if (profile.getPictureUrl() != null && !profile.getPictureUrl().isBlank()) {
				jteOutput.writeContent("\r\n          <img alt=\"avatar\" style=\"width:64px;height:64px;border-radius:50%\"");
				var __jte_html_attribute_1 = profile.getPictureUrl();
				if (gg.jte.runtime.TemplateUtils.isAttributeRendered(__jte_html_attribute_1)) {
					jteOutput.writeContent(" src=\"");
					jteOutput.setContext("img", "src");
					jteOutput.writeUserContent(__jte_html_attribute_1);
					jteOutput.setContext("img", null);
					jteOutput.writeContent("\"");
				}
				jteOutput.writeContent("/>\r\n        ");
			}
			jteOutput.writeContent("\r\n        <p><strong>Name:</strong> ");
			jteOutput.setContext("p", null);
			jteOutput.writeUserContent(profile.getName());
			jteOutput.writeContent("</p>\r\n        <p><strong>Email:</strong> ");
			jteOutput.setContext("p", null);
			jteOutput.writeUserContent(profile.getEmail());
			jteOutput.writeContent("</p>\r\n        <p><strong>Scopes:</strong>\r\n          ");
			if (profile.getScopes() == null || profile.getScopes().isEmpty()) {
				jteOutput.writeContent("\r\n            <em>none</em>\r\n          ");
			} else {
				jteOutput.writeContent("\r\n            ");
				for (int i = 0; i < profile.getScopes().size(); ++i) {
					jteOutput.writeContent("\r\n              ");
					jteOutput.setContext("p", null);
					jteOutput.writeUserContent(profile.getScopes().get(i));
					if (i < profile.getScopes().size() - 1) {
						jteOutput.writeContent(", ");
					}
					jteOutput.writeContent("\r\n            ");
				}
				jteOutput.writeContent("\r\n          ");
			}
			jteOutput.writeContent("\r\n        </p>\r\n      ");
		}
		jteOutput.writeContent("\r\n    </section>\r\n  </body>\r\n</html>\r\n");
	}
	public static void renderMap(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, java.util.Map<String, Object> params) {
		String title = (String)params.get("title");
		String greeting = (String)params.get("greeting");
		String name = (String)params.get("name");
		String cssUrl = (String)params.get("cssUrl");
		UserProfile profile = (UserProfile)params.get("profile");
		String userId = (String)params.get("userId");
		String error = (String)params.get("error");
		render(jteOutput, jteHtmlInterceptor, title, greeting, name, cssUrl, profile, userId, error);
	}
}
