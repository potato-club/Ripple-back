package org.example.rippleback.features.feed.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.rippleback.features.media.domain.Media;
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

    @Column(name = "author_id", nullable = false, updatable = false)
    private Long authorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", insertable = false, updatable = false)
    private User author;

    @Column(columnDefinition = "text", updatable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags_norm", columnDefinition = "text[]", nullable = false, updatable = false)
    @Builder.Default
    private String[] tagsNorm = new String[0];

    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

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

    @Column(name = "thumbnail_media_id")
    private Long thumbnailMediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thumbnail_media_id", insertable = false, updatable = false)
    private Media thumbnailMedia;

    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private List<FeedMedia> feedMedias = new ArrayList<>();

    public List<FeedMedia> getFeedMedias() {
        return List.copyOf(feedMedias);
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false, updatable = false)
    @Builder.Default
    private FeedVisibility visibility = FeedVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Builder.Default
    private FeedStatus status = FeedStatus.PUBLISHED;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public void softDelete() {
        if (this.status == FeedStatus.DELETED) return;
        this.status = FeedStatus.DELETED;
        this.deletedAt = Instant.now();
    }

    public void updateThumbnailMediaId(Long thumbnailMediaId) {
        this.thumbnailMediaId = thumbnailMediaId;
    }

    public void updateVisibility(FeedVisibility visibility) {
        this.visibility = visibility;
    }

    public void updateTagsNorm(List<String> tags) {
        this.tagsNorm = tags.toArray(String[]::new);
    }
}
