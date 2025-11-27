package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.FeedBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedBookmarkRepository extends JpaRepository<FeedBookmark, Long> {
    void deleteByUserId(Long userId);
}

