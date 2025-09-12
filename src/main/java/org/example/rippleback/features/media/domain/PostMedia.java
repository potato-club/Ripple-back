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
        name = "post_media",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_post_media_order", columnNames = {"post_id", "position"})
        },
        indexes = {
                @Index(name = "ix_post_media_post", columnList = "post_id, id DESC")
        }
)
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 포스트 */
    @Column(name = "post_id", nullable = false)
    private Long postId;

    /** 연결된 미디어 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    /** 이미지 정렬(0..14), 동영상은 NULL */
    @Column(name = "position")
    private Short position;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ", nullable = false, updatable = false)
    private Instant createdAt;

    /* TODO: 비즈니스 제약 - 포스트당 VIDEO는 최대 1개 (서비스 레벨에서 강제) */
    /* TODO: DB CHECK 제약 - position IS NULL OR position BETWEEN 0 AND 14 */
}
