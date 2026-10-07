package com.myorg.board;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.util.List;
import java.util.Map;

public class ListPostsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final PostRepository repository = new PostRepository(System.getenv("TABLE_NAME"));

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        int limit = parseLimit(event.getQueryStringParameters());
        try {
            List<PostResponse> posts = repository.listLatest(limit).stream()
                    .map(PostResponse::from)
                    .toList();
            return ApiResponses.json(200, posts);
        } catch (RuntimeException e) {
            context.getLogger().log("Failed to list posts: " + e);
            return ApiResponses.error(500, "Internal server error");
        }
    }

    private static int parseLimit(Map<String, String> params) {
        if (params == null || params.get("limit") == null) {
            return DEFAULT_LIMIT;
        }
        try {
            int value = Integer.parseInt(params.get("limit"));
            return Math.max(1, Math.min(value, MAX_LIMIT));
        } catch (NumberFormatException e) {
            return DEFAULT_LIMIT;
        }
    }
}