package org.example.rippleback.features.user.api.dto;

public record ProfileImagePresignResponseDto(
        String uploadUrl,
        String objectKey,
        Long maxSizeBytes
) {
}
