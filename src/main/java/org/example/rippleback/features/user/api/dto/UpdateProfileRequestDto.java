package org.example.rippleback.features.user.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDto(
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "영문/숫자/밑줄 3~20자")
        String username,
        @Size(max = 255)
        String profileMessage,
        @Size(max = 2048)
        String profileImageUrl
) {}