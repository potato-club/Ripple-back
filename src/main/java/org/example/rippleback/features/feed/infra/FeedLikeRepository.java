package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedLikeRepository extends JpaRepository<FeedLike,Long> {
    Optional<FeedLike> findByFeedIdAndUserId(Long feedId, Long userId);
    boolean existsByFeedIdAndUserId(Long feedId, Long userId);
    void deleteByUserId(Long userId);
}
