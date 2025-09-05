package zone.realfood;

import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.DomainNameOptions;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.SecurityPolicy;
import software.amazon.awscdk.services.certificatemanager.ICertificate;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.LogRetention;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.dynamodb.ITable;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.AaaaRecord;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGatewayDomain;
import software.constructs.Construct;

public class Backend extends Stack {

        public Backend(final Construct scope, final String id, final StackProps props, final IHostedZone zone, final ICertificate certificate,
                        final String subdomain, final ITable userProfileTable) {
                super(scope, id, props);

                // Construct the CSS URL based on the deterministic StaticContent bucket name
                String account = Stack.of(this).getAccount();
                String region = Stack.of(this).getRegion();
                String staticBucketName = String.format("realfood-zone-static-%s-%s", account, region);
                String cssUrl = String.format("https://%s.s3.%s.amazonaws.com/styles/style.css", staticBucketName, region);

                Function fn = Function.Builder.create(this, "MainFunction").runtime(Runtime.JAVA_21).architecture(Architecture.X86_64).memorySize(512)
                                .timeout(Duration.seconds(10)).handler("zone.realfood.MainHandler::handleRequest")
                                .code(Code.fromAsset("zone.realfood.backend/build/libs/zone.realfood.backend.jar"))
                                .environment(Map.of(
                                        "CSS_URL", cssUrl,
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

                LambdaRestApi api = LambdaRestApi.Builder
                                .create(this, "MainApi").handler(fn).proxy(true).domainName(DomainNameOptions.builder().domainName(subdomain)
                                                .certificate(certificate).endpointType(EndpointType.REGIONAL).securityPolicy(SecurityPolicy.TLS_1_2).build())
                                .build();

                ARecord.Builder.create(this, "BetaApiAliasA").zone(zone).recordName("beta")
                                .target(RecordTarget.fromAlias(new ApiGatewayDomain(api.getDomainName()))).build();

                AaaaRecord.Builder.create(this, "BetaApiAliasAAAA").zone(zone).recordName("beta")
                                .target(RecordTarget.fromAlias(new ApiGatewayDomain(api.getDomainName()))).build();

                CfnOutput.Builder.create(this, "ApiInvokeUrl").value(api.getUrl()).description("Default execute-api URL (for reference)").build();

                CfnOutput.Builder.create(this, "CustomDomainUrl").value("https://" + subdomain + "/")
                                .description("Custom domain for the API (REGIONAL, root path)").build();
        }
}
