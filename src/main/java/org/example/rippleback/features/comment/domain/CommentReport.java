package org.example.rippleback.features.comment.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "comment_report",
        indexes = {
                @Index(name = "ix_comment_report_user", columnList = "reporter_id"),
                @Index(name = "ix_comment_report_comment", columnList = "comment_id")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "reason", length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 12, nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.OPEN;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at", columnDefinition = "TIMESTAMPTZ")
    private Instant processedAt;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
