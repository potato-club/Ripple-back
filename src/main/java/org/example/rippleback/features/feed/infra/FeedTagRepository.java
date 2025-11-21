package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.FeedTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedTagRepository extends JpaRepository<FeedTag, Long> {
    Optional<FeedTag> findByName(String name);
}