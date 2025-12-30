package org.example.rippleback.features.feed.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.rippleback.features.feed.domain.FeedVisibility;
import org.example.rippleback.features.media.validation.S3ObjectKey;

import java.util.List;

public record FeedRequestDto(
        String content,
        List<String> tags,
        FeedVisibility visibility,
        List<@Valid FeedImagePatch> images,
        @Valid
        FeedVideoPatch video
) {
    public record FeedImagePatch(
            @NotBlank
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
    ) {}

    public record FeedVideoPatch(
            @NotBlank
            @S3ObjectKey(
                    mustStartWith = {"users/"},
                    allowPrefix = true
                    // 필요하면 정책적으로 아래도 추가 가능:
                    // , deniedExts = {"mp4","mov","mkv","webm"}
                    // , maxSegments = 10
                    // , segmentMaxLen = 100
            )
            String videoPrefix,
            Integer durationSec,
            String mimeType,
            Integer width,
            Integer height,
            Long sizeBytes,
            @NotNull @Valid
            FeedImagePatch thumbnail
    ) {}
}
