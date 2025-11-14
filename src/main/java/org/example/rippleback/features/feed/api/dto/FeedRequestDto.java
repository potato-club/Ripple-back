package org.example.rippleback.features.feed.api.dto;

import java.util.List;

public record FeedRequestDto(
        String content,
        List<String> mediaKeys
) {}
