package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

public class HelloHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String name = "world";
        if (input != null && input.getQueryStringParameters() != null) {
            name = input.getQueryStringParameters().getOrDefault("name", "world");
        }
        String body = """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="utf-8">
                <title>Hello, %s!</title>
              </head>
              <body>
                <p>Hello, %s!</p>
              </body>
            </html>
            """.formatted(name, name);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of("Content-Type", "text/html; charset=utf-8"))
                .withBody(body);
    }
}
