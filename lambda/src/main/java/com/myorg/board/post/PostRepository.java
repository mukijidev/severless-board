package com.myorg.board.post;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

public class PostRepository {
    private final DynamoDbTable<Post> table;
    private final DynamoDbIndex<Post> index;
    private static final String META = "META";

    public PostRepository(String tableName) {
        DynamoDbClient client = DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
        DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
        this.table = enhanced.table(tableName, TableSchema.fromBean(Post.class));
        this.index = table.index("gsi1");
    }

    public void save(Post post) {
        table.putItem(post);
    }

    public List<Post> listLatest(int limit) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue("POSTS").build()))
                .scanIndexForward(false)
                .limit(limit)
                .build();

        List<Post> posts = new ArrayList<>();
        for (Page<Post> page : index.query(request)) {
            posts.addAll(page.items());
            break;
        }
        return posts;
    }

    public UpdateResult update(String postId, String userId, String title, String content,
                               List<String> imageKeys) {
            Post change = new Post();
            change.setPk(partitionKey(postId));
            change.setSk(META);
            change.setTitle(title);
            change.setContent(content);
            change.setImageKeys(imageKeys);

            try {
                Post updated = table.updateItem(UpdateItemEnhancedRequest.builder(Post.class)
                        .item(change)
                        .ignoreNullsMode(IgnoreNullsMode.SCALAR_ONLY)
                        .conditionExpression(ownerCondition(userId))
                        .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
                        .build());
                return new UpdateResult(Outcome.OK, updated);
            } catch(ConditionalCheckFailedException e){
                return new UpdateResult(failureOutcome(e), null);
            }
    }


    public Outcome delete(String postId, String userId) {
        try {
            table.deleteItem(DeleteItemEnhancedRequest.builder()
                    .key(Key.builder().partitionValue(partitionKey(postId)).sortValue(META).build())
                    .conditionExpression(ownerCondition(userId))
                    .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
                    .build());
            return Outcome.OK;

        } catch (ConditionalCheckFailedException e ){
            return failureOutcome(e);
        }


    }

    private Outcome failureOutcome(ConditionalCheckFailedException  e) {
        return e.hasItem() ? Outcome.FORBIDDEN : Outcome.NOT_FOUND;
    }
    private static Expression ownerCondition(String userId) {
        return Expression.builder()
                .expression("attribute_exists(pk) AND authorId = :uid")
                .putExpressionValue(":uid", AttributeValue.fromS(userId))
                .build();
    }
    private static String partitionKey(String postId) {
        return "POST#" + postId;
    }
}