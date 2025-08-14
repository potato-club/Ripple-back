package org.example.rippleback.features.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(
        @NotBlank @Size(min = 8, max = 72) String currentPassword,
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {}
