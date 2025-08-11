package org.example.rippleback.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequestDto (
        @NotBlank String refreshToken,
        @NotBlank String deviceId
) {}
