package org.example.rippleback.features.user.api.dto;

public record UserSummaryDto(
        Long id,
        String username,
        String profileImageUrl
) {}