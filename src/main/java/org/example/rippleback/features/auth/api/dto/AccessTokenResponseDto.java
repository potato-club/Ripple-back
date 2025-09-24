package org.example.rippleback.features.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AccessTokenResponseDto(
        @Schema(description = "JWT Access Token")
        String accessToken
) {}
