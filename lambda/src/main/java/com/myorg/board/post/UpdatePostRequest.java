package com.myorg.board.post;

import java.util.List;

public record UpdatePostRequest(String title, String content,
                                List<String> imageKeys) {

}