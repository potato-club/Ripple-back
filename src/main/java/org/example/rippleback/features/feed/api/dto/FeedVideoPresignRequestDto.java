package org.example.rippleback.features.feed.api.dto;

public record FeedVideoPresignRequestDto(
        VideoSpec video,
        ThumbnailSpec thumbnail
) {
    public record VideoSpec(
            String mimeType,
            Long sizeBytes,
            Integer durationSec
    ) {}

    public record ThumbnailSpec(
            String mimeType,
            Long sizeBytes
    ) {}
}
