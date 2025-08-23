package com.myorg;

import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class App {
    public static void main(final String[] args) {
        software.amazon.awscdk.App app = new software.amazon.awscdk.App();

        String account = System.getenv("CDK_DEFAULT_ACCOUNT");
        String region  = System.getenv("CDK_DEFAULT_REGION");

        new HelloStack(app, "HelloStack", StackProps.builder()
                .env(Environment.builder().account(account).region(region).build())
                .build());

        app.synth();
    }
}
