package org.example.rippleback.features.feed.api.dto;

import java.util.List;

public record FeedPageDto(
        List<FeedResponseDto> feeds,
        Long nextCursor,
        boolean hasNest
) {
}
