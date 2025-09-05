package org.example.rippleback.features.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequestDto (
        @NotBlank(message = "디바이스 ID가 필요합니다.")
        String deviceId
) {}
