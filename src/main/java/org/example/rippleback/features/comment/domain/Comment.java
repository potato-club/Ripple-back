package org.example.rippleback.features.comment.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.user.domain.User;

import java.time.Instant;

@Entity
@Table(
        name = "comment",
        indexes = {
                @Index(name = "ix_comment_feed", columnList = "feed_id, id ASC"),
                @Index(name = "ix_comment_author", columnList = "author_id, id DESC"),
                @Index(name = "ix_comment_root", columnList = "root_comment_id, id ASC")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feed_id", nullable = false)
    private Long feedId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "root_comment_id")
    private Long rootCommentId;

    @Column(name = "to_user_id")
    private Long replyToUserId;

    @Column(name = "to_comment_id")
    private Long replyToCommentId;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    @Builder.Default
    private CommentStatus status = CommentStatus.PUBLISHED;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", length = 16, nullable = false)
    @Builder.Default
    private CommentVisibility visibility = CommentVisibility.VISIBLE;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMPTZ")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", insertable = false, updatable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", insertable = false, updatable = false)
    private Feed feed;

    public boolean isRoot() {
        return rootCommentId == null;
    }

    public boolean isVisible() {
        return visibility == CommentVisibility.VISIBLE;
    }

    public void markDeletedHidden() {
        this.status = CommentStatus.DELETED;
        this.visibility = CommentVisibility.HIDDEN;
        this.deletedAt = Instant.now();
    }

    public void markDeletedButVisibleWithMask() {
        this.status = CommentStatus.DELETED;
        this.visibility = CommentVisibility.VISIBLE;
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return this.status == CommentStatus.DELETED;
    }
}
