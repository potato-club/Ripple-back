package org.example.rippleback.features.comment.api.dto;

import org.example.rippleback.features.comment.domain.CommentReport;
import org.example.rippleback.features.comment.domain.CommentReportCategory;
import org.example.rippleback.features.comment.domain.CommentReportStatus;

import java.time.Instant;

public record CommentReportResponseDto(
        Long id,
        Long commentId,
        Long reporterId,
        CommentReportCategory category,
        CommentReportStatus status,
        String reason,
        Instant createdAt
) {
    public static CommentReportResponseDto from(CommentReport r) {
        return new CommentReportResponseDto(
                r.getId(),
                r.getCommentId(),
                r.getReporterId(),
                r.getCategory(),
                r.getStatus(),
                r.getReason(),
                r.getCreatedAt()
        );
    }
}
