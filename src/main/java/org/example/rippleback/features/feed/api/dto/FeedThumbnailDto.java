package org.example.rippleback.features.feed.api.dto;

public record FeedThumbnailDto(
        Long id,
        String thumbnailUrl,
        Long likeCount,
        Long bookmarkCount,
        Long commentCount
) {}

