package org.example.rippleback.features.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequestDto(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "영문/숫자/밑줄 3~20자")
        String username,
        @NotBlank String password,
        @NotBlank String deviceId
) {}