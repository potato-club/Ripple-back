package org.example.rippleback.features.comment.infra;

import org.example.rippleback.features.comment.domain.CommentReport;
import org.example.rippleback.features.comment.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import java.util.Optional;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    Optional<CommentReport> findByReporterIdAndCommentId(Long reporterId, Long commentId);

    boolean existsByReporterIdAndCommentId(Long reporterId, Long commentId);

    Page<CommentReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    Page<CommentReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    int deleteByReporterIdAndCommentId(Long reporterId, Long commentId);
}