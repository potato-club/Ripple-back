package org.example.rippleback.features.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfileImagePresignRequestDto(
        @NotBlank
        String mimeType,

        @NotNull
        Long sizeBytes
) {
}
