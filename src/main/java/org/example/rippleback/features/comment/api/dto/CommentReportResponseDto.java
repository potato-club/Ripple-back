package org.example.rippleback.features.comment.api.dto;

import org.example.rippleback.features.comment.domain.CommentReport;
import org.example.rippleback.features.comment.domain.ReportCategory;
import org.example.rippleback.features.comment.domain.ReportStatus;

import java.time.Instant;

public record CommentReportResponseDto(
        Long id,
        Long commentId,
        Long reporterId,
        ReportCategory category,
        ReportStatus status,
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
