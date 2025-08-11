package org.example.rippleback.features.auth.api.dto;

public record LoginResponseDto(
        String accessToken, String refreshToken
) {}