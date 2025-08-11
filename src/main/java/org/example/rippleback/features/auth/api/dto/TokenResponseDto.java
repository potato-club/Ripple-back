package org.example.rippleback.features.auth.api.dto;

public record TokenResponseDto (
        String accessToken,
        String refreshToken
){}
