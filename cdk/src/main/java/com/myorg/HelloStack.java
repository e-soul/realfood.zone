package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.apigateway.*;

public class HelloStack extends Stack {
    public HelloStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Function fn = Function.Builder.create(this, "HelloFunction")
                .runtime(Runtime.JAVA_21)
                .architecture(Architecture.X86_64)
                .memorySize(512)
                .timeout(Duration.seconds(10))
                .handler("com.example.HelloHandler::handleRequest")
                .code(Code.fromAsset("lambda/build/libs/hello-lambda.jar"))
                .build();

        LambdaRestApi api = LambdaRestApi.Builder.create(this, "HelloApi")
                .handler(fn)
                .proxy(true)
                .build();

        CfnOutput.Builder.create(this, "ApiUrl")
                .value(api.getUrl())
                .description("Invoke with: GET ${ApiUrl}?name=YourName")
                .build();
    }
}
