package org.example.rippleback.features.feed.api.dto;

public record FeedLikeResponseDto(
        Long feedId,
        Long likeCount,
        boolean liked
) {}
