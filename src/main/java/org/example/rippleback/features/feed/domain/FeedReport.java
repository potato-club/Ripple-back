package org.example.rippleback.features.feed.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.rippleback.features.user.domain.User;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "feed_reports", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_feed_reports_user_feed",
                columnNames = {"user_id", "feed_id"}
        )
})
public class FeedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feed_id", nullable = false)
    private Long feedId;

    @Column(name = "user_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FeedReportReason reason;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "created_at", columnDefinition = "TIMESTAMPZ", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
