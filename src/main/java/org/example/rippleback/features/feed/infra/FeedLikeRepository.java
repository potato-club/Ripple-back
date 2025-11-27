package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedLikeRepository extends JpaRepository<FeedLike,Long> {
    void deleteByUserId(Long userId);
}
