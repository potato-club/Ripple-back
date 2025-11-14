package org.example.rippleback.features.feed.api.dto;

import java.util.List;

public record FeedResponseDto(
        Long id,
        Long userId,
        String content,
        Long likeCount,
        List<String> mediaKeys
) {
}
