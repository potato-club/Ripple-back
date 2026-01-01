package org.example.rippleback.features.user.api.dto;

import org.example.rippleback.features.feed.api.dto.FeedResponseDto;

import java.util.List;

public record UserResponseDto(
        Long id,
        String username,
        String profileImageUrl,
        long postCount,
        long followerCount,
        long followingCount,
        List<FeedResponseDto> latestFeeds
) {}