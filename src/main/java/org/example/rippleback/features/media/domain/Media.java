package org.example.rippleback.features.media.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.rippleback.features.media.validation.S3ObjectKey;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Check(constraints = """
        (
          media_type IN ('PROFILE_IMAGE', 'FEED_IMAGE')
          AND object_key IS NOT NULL
          AND video_prefix IS NULL
          AND duration_sec IS NULL
        )
        OR
        (
          media_type = 'FEED_VIDEO'
          AND video_prefix IS NOT NULL
          AND object_key IS NULL
          AND duration_sec BETWEEN 3 AND 180
        )
        """)
@Entity
@Table(
        name = "media",
        indexes = {
                @Index(name = "ix_media_owner", columnList = "ownerId, id DESC")
        }
)
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이 미디어를 소유하고 있는 유저의 ID
    // Feed 와의 연결은 FeedMedia 를 통해서 관리
    @Column(name = "ownerId", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 20, nullable = false)
    private MediaType mediaType;

    @S3ObjectKey(
            allowedExts = {"jpg", "jpeg", "png", "webp", "avif"},
            allowPrefix = false
    )
    @Column(name = "object_key", length = 1024)
    private String objectKey;

    @S3ObjectKey(
            allowPrefix = true
    )
    @Column(name = "video_prefix", length = 1024)
    private String videoPrefix;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    private Integer width;

    private Integer height;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_status", length = 16, nullable = false)
    @Builder.Default
    private MediaStatus mediaStatus = MediaStatus.READY;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isProfileImage() {
        return mediaType == MediaType.PROFILE_IMAGE;
    }

    public boolean isFeedImage() {
        return mediaType == MediaType.FEED_IMAGE;
    }

    public boolean isFeedVideo() {
        return mediaType == MediaType.FEED_VIDEO;
    }

    public boolean isImage() {
        return mediaType == MediaType.PROFILE_IMAGE
                || mediaType == MediaType.FEED_IMAGE;
    }

    public boolean isVideo() {
        return mediaType == MediaType.FEED_VIDEO;
    }

    public boolean isReady() {
        return mediaStatus == MediaStatus.READY;
    }

    public void markUploading() {
        this.mediaStatus = MediaStatus.UPLOADING;
    }

    public void markProcessing() {
        this.mediaStatus = MediaStatus.PROCESSING;
    }

    public void markReady() {
        this.mediaStatus = MediaStatus.READY;
    }

    public void markFailed() {
        this.mediaStatus = MediaStatus.FAILED;
    }

    public static Media newProfileImage(
            Long userId,
            String objectKey,
            String mimeType,
            Integer width,
            Integer height,
            Long sizeBytes
    ) {
        return Media.builder()
                .ownerId(userId)
                .mediaType(MediaType.PROFILE_IMAGE)
                .objectKey(objectKey)
                .mimeType(mimeType)
                .width(width)
                .height(height)
                .sizeBytes(sizeBytes)
                .mediaStatus(MediaStatus.READY)
                .build();
    }

    public static Media newFeedImage(
            Long userId,
            String objectKey,
            String mimeType,
            Integer width,
            Integer height,
            Long sizeBytes
    ) {
        return Media.builder()
                .ownerId(userId)
                .mediaType(MediaType.FEED_IMAGE)
                .objectKey(objectKey)
                .mimeType(mimeType)
                .width(width)
                .height(height)
                .sizeBytes(sizeBytes)
                .mediaStatus(MediaStatus.READY)
                .build();
    }

    public static Media newFeedVideo(
            Long userId,
            String videoPrefix,
            String mimeType,
            Integer width,
            Integer height,
            Integer durationSec,
            Long sizeBytes
    ) {
        return Media.builder()
                .ownerId(userId)
                .mediaType(MediaType.FEED_VIDEO)
                .videoPrefix(videoPrefix)
                .mimeType(mimeType)
                .width(width)
                .height(height)
                .durationSec(durationSec)
                .sizeBytes(sizeBytes)
                .mediaStatus(MediaStatus.READY)
                .build();
    }
}