package org.example.rippleback.features.user.api.dto;

public record SignupResponseDto(
        Long id,
        String username,
        boolean emailVerified
) {}