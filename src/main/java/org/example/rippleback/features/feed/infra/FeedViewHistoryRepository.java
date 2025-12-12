package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.FeedViewHistory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedViewHistoryRepository extends JpaRepository<FeedViewHistory, Long> {
    boolean existsByUserIdAndFeedId(Long userId, Long feedId);
}

