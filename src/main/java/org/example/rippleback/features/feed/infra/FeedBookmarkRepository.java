package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.FeedBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedBookmarkRepository extends JpaRepository<FeedBookmark, Long> {
    Optional<FeedBookmark> findByFeedIdAndUserId(Long feedId, Long userId);
    boolean existsByFeedIdAndUserId(Long feedId, Long userId);
    void deleteByUserId(Long userId);
}

