package org.example.rippleback.features.user.api.dto;

public record UserResponseDto(
        Long id,
        String username,
        String profileMessage,
        Long profileMediaId,
        String profileImageUrl
) {
}
