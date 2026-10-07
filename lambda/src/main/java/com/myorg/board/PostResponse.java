package com.myorg.board;
import java.util.List;
import java.util.Objects;

public record PostResponse(String postId, String title, String content, String authorId, String createdAt,
                           List<String> imageKeys) {

    public static PostResponse from(Post post) {
        List<String> keys = Objects.requireNonNullElse(post.getImageKeys(), List.of());
        return new PostResponse(post.getPostId(), post.getTitle(), post.getContent(),
                post.getAuthorId(), post.getCreatedAt(), keys);
    }

}