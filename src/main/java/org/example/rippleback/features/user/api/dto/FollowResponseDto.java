package org.example.rippleback.features.user.api.dto;

public record FollowResponseDto(
        Long fromUserId,
        Long toUserId,
        boolean following
) {}