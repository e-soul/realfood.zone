package zone.realfood;

import java.util.List;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.CorsRule;
import software.amazon.awscdk.services.s3.HttpMethods;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.CacheControl;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;

public class StaticContent extends Stack {

    public StaticContent(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        String account = Stack.of(this).getAccount();
        String region = Stack.of(this).getRegion();
        String bucketName = String.format("realfood-zone-static-%s-%s", account, region);

        List<String> origins = List.of("https://realfood.zone", "https://www.realfood.zone", "https://beta.realfood.zone");
        Bucket bucket = Bucket.Builder.create(this, "StaticAssetsBucket").bucketName(bucketName).blockPublicAccess(BlockPublicAccess.BLOCK_ACLS_ONLY)
                .publicReadAccess(true).enforceSsl(true)
                .cors(List.of(CorsRule.builder().allowedOrigins(origins).allowedMethods(List.of(HttpMethods.GET, HttpMethods.HEAD)).allowedHeaders(List.of("*"))
                        .exposedHeaders(List.of("ETag")).maxAge(3000).build()))
                .build();

        BucketDeployment.Builder.create(this, "DeployStaticAssets").sources(List.of(Source.asset("static-content"))).destinationBucket(bucket)
                .cacheControl(List.of(CacheControl.maxAge(Duration.days(30)))).build();
    }
}
