package org.example.rippleback.features.feed.api.dto;

import org.example.rippleback.features.feed.domain.FeedStatus;
import org.example.rippleback.features.feed.domain.FeedVisibility;
import org.example.rippleback.features.user.api.dto.UserProfileSummaryResponseDto;

import java.time.Instant;

public record FeedResponseDto(
        Long id,
        UserProfileSummaryResponseDto author,
        String content,
        String[] tags,
        int likeCount,
        int bookmarkCount,
        int commentCount,
        int viewCount,
        String thumbnailUrl,
        FeedStatus feedStatus,
        FeedVisibility visibility,
        Instant createdAt
) {}
