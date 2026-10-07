package com.myorg.board;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CreatePostHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final PostRepository repository = new PostRepository(System.getenv("TABLE_NAME"));

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String body = event.getBody();
        if (body == null || body.isBlank()) {
            return ApiResponses.error(400, "Request body is required");
        }

        CreatePostRequest request;
        try {
            request = ApiResponses.mapper().readValue(body, CreatePostRequest.class);
        } catch (JsonProcessingException e) {
            return ApiResponses.error(400, "Invalid JSON body");
        }

        if (request == null || isBlank(request.title()) || isBlank(request.content())) {
            return ApiResponses.error(400, "title and content are required");
        }
        if (request.title().length() > 100) {
            return ApiResponses.error(400, "title must be 100 characters or less");
        }

        String postId = UUID.randomUUID().toString();
        String createdAt = TIMESTAMP.format(Instant.now());

        Post post = new Post();
        post.setPk("POST#" + postId);
        post.setSk("META");
        post.setGsi1pk("POSTS");
        post.setGsi1sk(createdAt + "#" + postId);
        post.setPostId(postId);
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setAuthorId("anonymous");
        post.setCreatedAt(createdAt);

        try {
            repository.save(post);
        } catch (RuntimeException e) {
            context.getLogger().log("Failed to save post: " + e);
            return ApiResponses.error(500, "Internal server error");
        }
        return ApiResponses.json(201, PostResponse.from(post));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}