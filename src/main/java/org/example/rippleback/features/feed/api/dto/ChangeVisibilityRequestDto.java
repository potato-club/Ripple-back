package org.example.rippleback.features.feed.api.dto;

import org.example.rippleback.features.feed.domain.FeedVisibility;

public record ChangeVisibilityRequestDto(
        FeedVisibility visibility
) {}
