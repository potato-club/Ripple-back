package org.example.rippleback.features.feed.api.dto;

import java.util.List;

public record FeedImagePresignResponseDto(
        List<Item> items
) {
    public record Item(
            String uploadUrl,
            String objectKey,
            long maxSizeBytes
    ) {}
}
