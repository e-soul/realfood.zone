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
import software.amazon.awscdk.services.apigateway.DomainNameOptions;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.SecurityPolicy;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.AaaaRecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGatewayDomain;
import software.amazon.awscdk.services.logs.RetentionDays;

public class HelloStack extends Stack {
    public HelloStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        IHostedZone zone = HostedZone.fromLookup(this, "RealfoodZone",
            HostedZoneProviderProps.builder()
                .domainName("realfood.zone")
                .privateZone(false)
                .build());

        String subdomain = "beta.realfood.zone";
        DnsValidatedCertificate cert = DnsValidatedCertificate.Builder.create(this, "BetaApiCert")
            .domainName(subdomain)
            .hostedZone(zone)
            .build();

        Function fn = Function.Builder.create(this, "HelloFunction")
                .runtime(Runtime.JAVA_21)
                .architecture(Architecture.X86_64)
                .memorySize(512)
                .timeout(Duration.seconds(10))
                .handler("com.example.HelloHandler::handleRequest")
                .code(Code.fromAsset("lambda/build/libs/hello-lambda.jar"))
                .logRetention(RetentionDays.THREE_DAYS)
                .build();

        LambdaRestApi api = LambdaRestApi.Builder.create(this, "HelloApi")
                .handler(fn)
                .proxy(true)
                .domainName(DomainNameOptions.builder()
                    .domainName(subdomain)
                    .certificate(cert)
                    .endpointType(EndpointType.REGIONAL)
                    .securityPolicy(SecurityPolicy.TLS_1_2)
                    .build())
                .build();

        ARecord.Builder.create(this, "BetaApiAliasA")
            .zone(zone)
            .recordName("beta")
            .target(RecordTarget.fromAlias(new ApiGatewayDomain(api.getDomainName())))
            .build();

        AaaaRecord.Builder.create(this, "BetaApiAliasAAAA")
            .zone(zone)
            .recordName("beta")
            .target(RecordTarget.fromAlias(new ApiGatewayDomain(api.getDomainName())))
            .build();

        CfnOutput.Builder.create(this, "ApiInvokeUrl")
            .value(api.getUrl())
            .description("Default execute-api URL (for reference)")
            .build();

        CfnOutput.Builder.create(this, "CustomDomainUrl")
            .value("https://" + subdomain + "/")
            .description("Custom domain for the API (REGIONAL, root path)")
            .build();
    }
}
