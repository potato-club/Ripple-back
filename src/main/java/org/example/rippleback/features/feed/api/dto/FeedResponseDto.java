package org.example.rippleback.features.feed.api.dto;

import org.example.rippleback.features.feed.domain.FeedStatus;
import org.example.rippleback.features.feed.domain.FeedVisibility;

import java.time.Instant;
import java.util.List;

public record FeedResponseDto(
        Long id,
        Long authorId,
        String content,
        String thumbnail,
        List<String> mediaUrls,
        String[] tags,
        int likeCount,
        int bookmarkCount,
        FeedVisibility visibility,
        FeedStatus status,
        Instant createdAt,
        Instant updatedAt
) { }
