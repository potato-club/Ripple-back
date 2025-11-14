package org.example.rippleback.features.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequestDto (
        @NotBlank(message = "리프레시 토큰이 필요합니다.")
        String refreshToken,

        @NotBlank(message = "디바이스 ID가 필요합니다.")
        String deviceId
) {}
