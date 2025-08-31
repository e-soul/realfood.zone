package zone.realfood;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class MainHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    String name = "world";
    if (input != null && input.getQueryStringParameters() != null) {
      name = input.getQueryStringParameters().getOrDefault("name", "world");
    }
    String cssUrl = System.getenv("CSS_URL");
    String cssLink = "<link rel=\"stylesheet\" href=\"" + cssUrl + "\">";
    String body = """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="utf-8">
          %s
                <title>Hello, %s!</title>
              </head>
              <body>
                <h3>Hello, %s!</h3>
          <p>v7</p>
              </body>
            </html>
        """.formatted(cssLink, name, name);
    return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(Map.of("Content-Type", "text/html; charset=utf-8")).withBody(body);
  }
}
