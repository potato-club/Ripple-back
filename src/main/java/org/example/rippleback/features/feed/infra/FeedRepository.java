package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.Feed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRepository extends JpaRepository<Feed, Long> {}
