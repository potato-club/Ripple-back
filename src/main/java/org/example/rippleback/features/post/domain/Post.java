package org.example.rippleback.features.post.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import org.example.rippleback.features.user.domain.User;

@Entity
@Table(name = "posts",
        indexes = {
                @Index(name = "ix_posts_author", columnList = "author_id, id DESC"),
                @Index(name = "ix_posts_status", columnList = "status, id DESC")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", insertable = false, updatable = false)
    private User author;

    @Column(columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Builder.Default
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "image_count", nullable = false)
    @Builder.Default
    private short imageCount = 0;

    @Column(name = "has_video", nullable = false)
    @Builder.Default
    private boolean hasVideo = false;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(name = "bookmark_count", nullable = false)
    @Builder.Default
    private int bookmarkCount = 0;

    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private int commentCount = 0;

    /** PostgreSQL text[] 매핑 */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags_norm", columnDefinition = "text[]", nullable = false)
    @Builder.Default
    private String[] tagsNorm = new String[0];

    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMPTZ")
    private Instant deletedAt;

    /* ====== 도메인 로직(필요 최소한) ====== */

    @PreUpdate
    void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public void publish() {
        this.status = PostStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.status = PostStatus.DELETED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void changeVisibility(PostVisibility visibility) {
        this.visibility = visibility;
        this.updatedAt = Instant.now();
    }

    public void setMediaCounts(int imageCount, boolean hasVideo) {
        if (imageCount < 0 || imageCount > 15) {
            throw new IllegalArgumentException("image_count must be between 0 and 15");
        }
        this.imageCount = (short) imageCount;
        this.hasVideo = hasVideo;
        this.updatedAt = Instant.now();
    }

    public void incLike()      { this.likeCount      = Math.max(0, this.likeCount + 1); }
    public void decLike()      { this.likeCount      = Math.max(0, this.likeCount - 1); }
    public void incBookmark()  { this.bookmarkCount  = Math.max(0, this.bookmarkCount + 1); }
    public void decBookmark()  { this.bookmarkCount  = Math.max(0, this.bookmarkCount - 1); }
    public void incComment()   { this.commentCount   = Math.max(0, this.commentCount + 1); }
    public void decComment()   { this.commentCount   = Math.max(0, this.commentCount - 1); }

    public boolean isPublished() { return this.status == PostStatus.PUBLISHED; }
    public boolean isDeleted()   { return this.status == PostStatus.DELETED; }
}
