package org.example.rippleback.features.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationSendRequestDto(
        @NotBlank @Email String email
) {}