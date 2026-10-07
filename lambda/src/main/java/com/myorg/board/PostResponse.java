package com.myorg.board;

public record PostResponse(String postId, String title, String content, String authorId, String createdAt) {

    public static PostResponse from(Post post) {
        return new PostResponse(post.getPostId(), post.getTitle(), post.getContent(),
                post.getAuthorId(), post.getCreatedAt());
    }

}