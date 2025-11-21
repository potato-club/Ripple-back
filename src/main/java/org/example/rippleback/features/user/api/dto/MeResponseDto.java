package org.example.rippleback.features.user.api.dto;

import java.time.Instant;

public record MeResponseDto(
        Long id,
        String username,
        String email,
        boolean emailVerified,
        String profileImageUrl,
        String status,          // "ACTIVE" | "SUSPENDED" | "DELETED"
        Long tokenVersion,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {}