package org.example.rippleback.domain.auth.dto;

public record TokenResponseDto (
        String accessToken,
        String refreshToken
){}
