package org.example.rippleback.features.feed.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.rippleback.features.user.domain.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Table(name = "feeds",
        indexes = {
                @Index(name = "ix_feeds_author", columnList = "author_id, id DESC"),
                @Index(name = "ix_feeds_status", columnList = "status, id DESC")
        })
public class Feed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", insertable = false, updatable = false)
    private User author;

    @Column(columnDefinition = "text")
    private String content;

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

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "bookmark_count", nullable = false)
    private int bookmarkCount = 0;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "feed_thumbnail")
    private String thumbnail;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "feed_media_keys", joinColumns = @JoinColumn(name = "feed_id"))
    @Column(name = "media_key")
    private List<String> mediaKeys;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Builder.Default
    private FeedVisibility visibility = FeedVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Builder.Default
    private FeedStatus status = FeedStatus.PUBLISHED;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }

        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
    }

    @PreUpdate
    public void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.status = FeedStatus.DELETED;
        this.deletedAt = Instant.now();
        touchUpdatedAt();
    }

    public void updateVisibility(FeedVisibility visibility) {
        this.visibility = visibility;
        touchUpdatedAt();
    }

    public void updateTagsNorm(List<String> tags) {
        this.tagsNorm = tags.toArray(String[]::new);
    }

}
