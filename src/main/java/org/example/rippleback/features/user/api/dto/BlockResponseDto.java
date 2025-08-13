package org.example.rippleback.features.user.api.dto;

public record BlockResponseDto(
        Long fromUserId,
        Long toUserId,
        boolean blocked
) {}