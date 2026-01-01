package org.example.rippleback.features.user.api.dto;

import org.example.rippleback.features.feed.api.dto.FeedResponseDto;

import java.util.List;

public record UserProfileResponseDto(
        Long id,
        String username,
        String profileImageUrl,
        boolean following,
        long postCount,
        long followerCount,
        long followingCount,
        List<FeedResponseDto> latestFeeds
) {}