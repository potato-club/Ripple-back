package org.example.rippleback.features.user.api.dto;

import org.example.rippleback.features.feed.api.dto.FeedResponseDto;

import java.time.Instant;
import java.util.List;

public record MeResponseDto(
        Long id,
        String username,
        String email,
        boolean emailVerified,
        String profileImageUrl,
        String status,
        Long tokenVersion,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        long postCount,
        long followerCount,
        long followingCount,
        List<FeedResponseDto> latestFeeds
) {}