package org.example.rippleback.features.feed.api.dto;


import org.example.rippleback.features.feed.domain.FeedVisibility;

import java.util.List;

public record FeedRequestDto(
        String content,
        List<String> mediaKeys,
        List<String> tags,
        FeedVisibility visibility
) { }