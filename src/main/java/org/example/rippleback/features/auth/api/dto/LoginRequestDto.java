package org.example.rippleback.features.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String deviceId
) {}