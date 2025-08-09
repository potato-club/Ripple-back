package org.example.rippleback.domain.auth.dto;

public record LoginResponseDto(
        String accessToken, String refreshToken
) {}