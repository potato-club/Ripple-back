package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.FeedReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedReportRepository extends JpaRepository<FeedReport, Long> {
    boolean existsByUserIdAndFeedId(Long userId, Long feedId);
}
