package org.example.rippleback.features.feed.api.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record FeedFullViewDto(
        Long id,
        Long authorId,
        String authorName,
        String content,
        List<String> imageUrls,
        String videoHlsUrl,
        String videoSourceUrl,
        List<String> tags,
        int likeCount,
        int bookmarkCount,
        int viewCount,
        boolean liked,
        boolean bookmarked,
        Instant createdAt
) {}
