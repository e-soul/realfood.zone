package zone.realfood;

import java.util.List;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
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

        Bucket bucket = Bucket.Builder.create(this, "StaticAssetsBucket")
                .bucketName(bucketName)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ACLS_ONLY)
                .publicReadAccess(true)
                .enforceSsl(true)
                .build();

    BucketDeployment.Builder.create(this, "DeployStaticAssets")
        // Deploy everything from the consolidated static-content folder
        .sources(List.of(Source.asset("static-content")))
                .destinationBucket(bucket)
                .cacheControl(List.of(CacheControl.maxAge(Duration.days(30))))
                .build();
    }
}
