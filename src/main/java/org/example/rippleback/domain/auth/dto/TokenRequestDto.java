package org.example.rippleback.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TokenRequestDto (
        @Email @NotBlank String email,
        @NotBlank String refreshToken
) {}
