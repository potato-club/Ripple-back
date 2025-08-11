package org.example.rippleback.features.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequestDto (
        @NotBlank String refreshToken,
        @NotBlank String deviceId
) {}
