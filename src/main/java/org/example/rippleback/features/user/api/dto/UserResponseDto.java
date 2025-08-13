package org.example.rippleback.features.user.api.dto;

public record UserResponseDto(
        Long id,
        String username,
        String profileImageUrl,
        String profileMessage,
        String status
) {}