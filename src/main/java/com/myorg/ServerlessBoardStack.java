package com.myorg;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import java.util.Map;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.EventType;
import software.amazon.awscdk.services.s3.notifications.LambdaDestination;

public class ServerlessBoardStack extends Stack {
    public ServerlessBoardStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ServerlessBoardStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here
        final Bucket bucket = Bucket.Builder.create(this, "BoardBucket")
                .removalPolicy(RemovalPolicy.DESTROY)
                .autoDeleteObjects(true)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .build();

        final LogGroup uploadLogs = LogGroup.Builder.create(this, "UploadFunctionLogs")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        final Function uploadFn = Function.Builder.create(this, "UploadFunction")
                .runtime(Runtime.JAVA_21)
                .handler("com.myorg.board.UploadHandler::handleRequest")
                .code(Code.fromAsset("lambda/target/board-lambda.jar"))
                .memorySize(512)
                .timeout(Duration.seconds(15))
                .environment(Map.of("BUCKET_NAME", bucket.getBucketName()))
                .logGroup(uploadLogs)
                .build();

        bucket.grantReadWrite(uploadFn);
        bucket.addEventNotification(EventType.OBJECT_CREATED, new LambdaDestination(uploadFn));
    }
}
