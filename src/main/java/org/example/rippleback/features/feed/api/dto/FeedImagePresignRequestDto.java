package org.example.rippleback.features.feed.api.dto;

import java.util.List;

public record FeedImagePresignRequestDto(
        List<FileSpec> files
) {
    public record FileSpec(
            String mimeType,
            Long sizeBytes
    ) {}
}
