package org.example.rippleback.features.feed.api.dto;

public record FeedVideoPresignResponseDto(
        String videoPrefix,
        String videoObjectKey,
        String videoUploadUrl,
        String thumbnailObjectKey,
        String thumbnailUploadUrl,
        long maxVideoSizeBytes,
        long maxThumbnailSizeBytes,
        int minDurationSec,
        int maxDurationSec
) {}
