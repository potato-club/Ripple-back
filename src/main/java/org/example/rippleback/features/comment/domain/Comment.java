package org.example.rippleback.features.comment.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.rippleback.features.post.domain.Post;
import org.example.rippleback.features.user.domain.User;

import java.time.Instant;

@Entity
@Table(
        name = "comment",
        indexes = {
                @Index(name = "ix_comment_post", columnList = "post_id, id ASC"),
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

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "root_comment_id")
    private Long rootCommentId;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    @Builder.Default
    private CommentStatus status = CommentStatus.PUBLISHED;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMPTZ")
    private Instant deletedAt;

    /* ===== 읽기전용 연관(선택) ===== */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", insertable = false, updatable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;

    /* ===== 도메인 편의 메서드 ===== */
    public void softDelete() {
        this.status = CommentStatus.DELETED;
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return this.status == CommentStatus.DELETED;
    }
}
