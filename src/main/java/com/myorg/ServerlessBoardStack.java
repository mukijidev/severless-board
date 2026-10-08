package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.Table;
import java.util.Map;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.StageOptions;


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

        final Table table = Table.Builder.create(this, "BoardTable")
                .partitionKey(Attribute.builder().name("pk").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("sk").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        table.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("gsi1")
                .partitionKey(Attribute.builder().name("gsi1pk").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("gsi1sk").type(AttributeType.STRING).build())
                .build());

        final Function createPostFn = createBoardFunction(
                "CreatePostFunction", "com.myorg.board.post.CreatePostHandler::handleRequest",  Map.of("TABLE_NAME", table.getTableName()));
        final Function listPostsFn = createBoardFunction(
                "ListPostsFunction", "com.myorg.board.post.ListPostsHandler::handleRequest",  Map.of("TABLE_NAME", table.getTableName()));

        table.grant(createPostFn, "dynamodb:PutItem");
        table.grant(listPostsFn, "dynamodb:Query");

        final Function presignedUrlFn = createBoardFunction(
                "PresignedUrlFunction",  "com.myorg.board.upload.PresignedUrlHandler::handleRequest",
                Map.of("BUCKET_NAME", bucket.getBucketName()));

        bucket.grantPut(presignedUrlFn, "uploads/*");


        final RestApi api = RestApi.Builder.create(this, "BoardApi")
                .restApiName("serverless-board-api")
                .description("Serverless board REST API")
                .deployOptions(StageOptions.builder()
                        .stageName("prod")
                        .throttlingRateLimit(5)
                        .throttlingBurstLimit(10)
                        .build())
                .build();

        final Resource posts = api.getRoot().addResource("posts");
        posts.addMethod("GET", new LambdaIntegration(listPostsFn));
        posts.addMethod("POST", new LambdaIntegration(createPostFn));

        final Resource uploads = api.getRoot().addResource("uploads");
        uploads.addMethod("POST", new LambdaIntegration(presignedUrlFn));

        CfnOutput.Builder.create(this, "ApiUrl")
                .value((api.getUrl()))
                .description("Base URL of the board  API")
                .build();

    }

    private Function createBoardFunction(String id, String handler, Map<String, String> environment) {
        final LogGroup logs = LogGroup.Builder.create(this, id + "Logs")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        return Function.Builder.create(this, id)
                .runtime(Runtime.JAVA_21)
                .handler(handler)
                .code(Code.fromAsset("lambda/target/board-lambda.jar"))
                .memorySize(512)
                .timeout(Duration.seconds(15))
                .logGroup(logs)
                .environment(environment)
                .build();
    }
}
