package org.example.rippleback.features.media.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Entity
@Table(
        name = "media",
        indexes = {
                @Index(name = "ix_media_owner", columnList = "owner_id, id DESC")
        }
)
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 10, nullable = false)
    private MediaType mediaType;

    /** 이미지 전용: object key (예: users/{id}/profile/{uuid}.jpg) */
    @Column(name = "object_key", length = 512)
    private String objectKey;

    /** 동영상 전용: video prefix (예: posts/{postId}/videos/{mediaId}) */
    @Column(name = "video_prefix", length = 512)
    private String videoPrefix;

    /** 메타(선택 저장) */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    private Integer width;

    private Integer height;

    @Column(name = "duration_sec")
    private Short durationSec;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_status", length = 16, nullable = false)
    @Builder.Default
    private MediaStatus mediaStatus = MediaStatus.READY;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isImage() { return mediaType == MediaType.IMAGE; }
    public boolean isVideo() { return mediaType == MediaType.VIDEO; }

    /* TODO: DB CHECK 제약
       (media_type='IMAGE' AND object_key IS NOT NULL AND video_prefix IS NULL)
       OR (media_type='VIDEO' AND video_prefix IS NOT NULL AND object_key IS NULL)
    */
}
