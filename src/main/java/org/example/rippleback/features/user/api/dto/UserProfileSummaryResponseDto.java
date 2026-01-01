package org.example.rippleback.features.user.api.dto;

public record UserProfileSummaryResponseDto(
        Long id,
        String username,
        String profileImageUrl,
        boolean following
) {}