package org.example.rippleback.features.user.api.dto;

import jakarta.validation.constraints.*;

public record SignupRequestDto(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "영문/숫자/밑줄 3~20자")
        String username,
        @NotBlank @Email
        String email,
        @NotBlank @Size(min = 8, max = 72)
        String password,
        @NotBlank @Size(min = 6, max = 6)
        String emailCode
) {}