package zone.realfood;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigatewayv2.CfnApi;
import software.amazon.awscdk.services.apigatewayv2.CfnApiMapping;
import software.amazon.awscdk.services.apigatewayv2.CfnDomainName;
import software.amazon.awscdk.services.apigatewayv2.CfnIntegration;
import software.amazon.awscdk.services.apigatewayv2.CfnRoute;
import software.amazon.awscdk.services.apigatewayv2.CfnStage;
import software.amazon.awscdk.services.certificatemanager.ICertificate;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Permission;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.LogRetention;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.dynamodb.ITable;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.AaaaRecord;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGatewayv2DomainProperties;
import software.constructs.Construct;

public class Backend extends Stack {

        private static final String SHADOW_JAR_PATH = "zone.realfood.backend/build/libs/zone.realfood.backend-all.jar";

        public Backend(final Construct scope, final String id, final StackProps props, final IHostedZone zone, final ICertificate certificate,
                        final String subdomain, final ITable userProfileTable) {
                super(scope, id, props);

                // Construct the static base URL based on the deterministic StaticContent bucket name
                String account = Stack.of(this).getAccount();
                String region = Stack.of(this).getRegion();
                String staticBucketName = String.format("realfood-zone-static-%s-%s", account, region);
                String staticBaseUrl = String.format("https://%s.s3.%s.amazonaws.com", staticBucketName, region);

                Function fn = Function.Builder.create(this, "MainFunction").runtime(Runtime.JAVA_21).architecture(Architecture.X86_64).memorySize(512)
                                .timeout(Duration.seconds(10)).handler("zone.realfood.MainHandler::handleRequest")
                                .code(Code.fromAsset(SHADOW_JAR_PATH))
                                .environment(Map.of(
                                        "STATIC_BASE_URL", staticBaseUrl,
                                        "USER_PROFILE_TABLE", userProfileTable.getTableName(),
                                        "GOOGLE_CLIENT_ID", System.getenv().getOrDefault("GOOGLE_CLIENT_ID", ""),
                                        "GOOGLE_CLIENT_SECRET", System.getenv().getOrDefault("GOOGLE_CLIENT_SECRET", ""),
                                        "GOOGLE_REDIRECT_URI", System.getenv().getOrDefault("GOOGLE_REDIRECT_URI", "")
                                ))
                                .build();

                // Allow the Lambda to read and write the user profile table (for sample seeding)
                userProfileTable.grantReadWriteData(fn);

                LogRetention.Builder.create(this, "MainFunctionLogRetention").logGroupName("/aws/lambda/" + fn.getFunctionName())
                                .retention(RetentionDays.THREE_DAYS).build();

                String integrationUri = String.format("arn:aws:apigateway:%s:lambda:path/2015-03-31/functions/%s/invocations", region,
                                fn.getFunctionArn());

                CfnApi httpApi = CfnApi.Builder.create(this, "MainApi").name("MainApi").protocolType("HTTP").build();

                CfnIntegration integration = CfnIntegration.Builder.create(this, "MainIntegration").apiId(httpApi.getAttrApiId())
                                .integrationType("AWS_PROXY").integrationMethod("POST").integrationUri(integrationUri)
                                .payloadFormatVersion("2.0").build();

                CfnRoute.Builder.create(this, "DefaultRoute").apiId(httpApi.getAttrApiId()).routeKey("$default")
                                .target("integrations/" + integration.getRef()).build();

                CfnStage.Builder.create(this, "DefaultStage").apiId(httpApi.getAttrApiId()).stageName("$default").autoDeploy(true)
                                .build();

                fn.addPermission("HttpApiInvokePermission",
                                Permission.builder().principal(new ServicePrincipal("apigateway.amazonaws.com")).sourceArn(
                                                String.format("arn:aws:execute-api:%s:%s:%s/*/*/*", region, account, httpApi.getAttrApiId()))
                                                .build());

                CfnDomainName domainName = CfnDomainName.Builder.create(this, "MainApiDomain").domainName(subdomain)
                                .domainNameConfigurations(List.of(CfnDomainName.DomainNameConfigurationProperty.builder()
                                                .certificateArn(certificate.getCertificateArn()).endpointType("REGIONAL")
                                                .securityPolicy("TLS_1_2").build()))
                                .build();

                CfnApiMapping apiMapping = CfnApiMapping.Builder.create(this, "DefaultMapping").apiId(httpApi.getAttrApiId()).domainName(subdomain)
                                .stage("$default").build();
                apiMapping.addDependency(domainName);

                ARecord.Builder.create(this, "BetaApiAliasA").zone(zone).recordName("beta")
                                .target(RecordTarget.fromAlias(new ApiGatewayv2DomainProperties(domainName.getAttrRegionalDomainName(),
                                                domainName.getAttrRegionalHostedZoneId())))
                                .build();

                AaaaRecord.Builder.create(this, "BetaApiAliasAAAA").zone(zone).recordName("beta")
                                .target(RecordTarget.fromAlias(new ApiGatewayv2DomainProperties(domainName.getAttrRegionalDomainName(),
                                                domainName.getAttrRegionalHostedZoneId())))
                                .build();

                CfnOutput.Builder.create(this, "ApiInvokeUrl").value(httpApi.getAttrApiEndpoint())
                                .description("Default execute-api URL (HTTP API v2)").build();

                CfnOutput.Builder.create(this, "CustomDomainUrl").value("https://" + subdomain + "/")
                                .description("Custom domain for the API (REGIONAL, root path)").build();
        }
}
