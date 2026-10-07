package com.myorg.board;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class PostRepository {
    private final DynamoDbTable<Post> table;
    private final DynamoDbIndex<Post> index;

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
}