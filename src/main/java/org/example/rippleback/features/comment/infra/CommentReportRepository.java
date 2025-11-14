package org.example.rippleback.features.comment.infra;

import org.example.rippleback.features.comment.domain.CommentReport;
import org.example.rippleback.features.comment.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.awt.print.Pageable;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    Optional<CommentReport> findByReporterIdAndCommentIdAndStatusIn(
            Long reporterId, Long commentId, Collection<ReportStatus> statuses);

    @Query("select count(r) from CommentReport r where r.commentId = :commentId and r.status in :statuses")
    long countOpenByCommentId(@Param("commentId") Long commentId,
                              @Param("statuses") Collection<ReportStatus> statuses);

    Page<CommentReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @Modifying
    @Query("""
            update CommentReport r
               set r.status = :status,
                   r.processedAt = :processedAt,
                   r.notes = :notes
             where r.id = :id
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") ReportStatus status,
                     @Param("processedAt") Instant processedAt,
                     @Param("notes") String notes);
}
