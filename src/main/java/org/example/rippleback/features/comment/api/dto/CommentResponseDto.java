package org.example.rippleback.features.comment.api.dto;

import org.example.rippleback.features.comment.domain.Comment;
import org.example.rippleback.features.comment.domain.CommentStatus;
import org.example.rippleback.features.user.api.dto.UserSummaryDto;

import java.time.Instant;

public record CommentResponseDto(
        Long id,
        UserSummaryDto author,
        Long rootCommentId,
        Long replyToUserId,
        Long replyToCommentId,
        String content,
        int likeCount,
        boolean deleted,
        Instant createdAt
) {
    public static CommentResponseDto from(Comment c, UserSummaryDto authorSummary) {
        String safeContent = c.getStatus() == CommentStatus.DELETED
                ? "삭제된 상태입니다."
                : c.getContent();

        return new CommentResponseDto(
                c.getId(),
                authorSummary,
                c.getRootCommentId(),
                c.getReplyToUserId(),
                c.getReplyToCommentId(),
                safeContent,
                c.getLikeCount(),
                c.getStatus() == CommentStatus.DELETED,
                c.getCreatedAt()
        );
    }
}
