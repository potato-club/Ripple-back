package org.example.rippleback.features.user.api.dto;

import jakarta.validation.Valid;
import org.example.rippleback.features.media.validation.S3ObjectKey;

public record UpdateProfileRequestDto(
        String username,
        @Valid ProfileImagePatch profileImage
) {
    public record ProfileImagePatch(
            Action action,
            @S3ObjectKey(
                    mustStartWith = {"users/"},
                    allowedExts = {"jpg", "jpeg", "png", "webp", "avif"},
                    allowPrefix = false
            )
            String objectKey,
            String mimeType,
            Integer width,
            Integer height,
            Long sizeBytes
    ) {
        public enum Action {
            KEEP,
            SET,
            CLEAR
        }
    }
}
