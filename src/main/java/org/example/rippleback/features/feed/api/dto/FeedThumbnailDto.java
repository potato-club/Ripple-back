package org.example.rippleback.features.feed.api.dto;

public record ReelsThumbnailDto(
        Long id,
        String thumbnailUrl,
        Long likeCount,
        Long bookmarkCount,
        Long commentCount
) {}

