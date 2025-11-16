package org.example.rippleback.features.comment.api.dto;

import org.example.rippleback.features.comment.domain.Comment;
import org.example.rippleback.features.comment.domain.CommentStatus;

import java.time.Instant;

public record CommentResponseDto(
        Long id,
        Long feedId,
        Long authorId,
        Long parentCommentId,
        Long rootCommentId,
        String content,
        int likeCount,
        boolean deleted,
        Instant createdAt
) {
    public static CommentResponseDto from(Comment c) {
        return new CommentResponseDto(
                c.getId(),
                c.getFeedId(),
                c.getAuthorId(),
                c.getParentCommentId(),
                c.getRootCommentId(),
                c.getContent(),
                c.getLikeCount(),
                c.getStatus() == CommentStatus.DELETED,
                c.getCreatedAt()
        );
    }
}