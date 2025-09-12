package org.example.rippleback.features.user.api.dto;

public record UserResponseDto(
        Long id,
        String username,
        String profileImageUrl,
        long postCount,
        long followerCount,
        long followingCount
) {}