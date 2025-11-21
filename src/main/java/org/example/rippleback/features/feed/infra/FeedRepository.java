package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.domain.FeedStatus;
import org.example.rippleback.features.feed.domain.FeedVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {
    long countByAuthorIdAndStatus(Long id, FeedStatus feedStatus);
}
