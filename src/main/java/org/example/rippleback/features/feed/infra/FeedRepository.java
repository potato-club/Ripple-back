package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.domain.FeedStatus;
import org.example.rippleback.features.feed.domain.FeedVisibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {
    long countByAuthorIdAndStatus(Long id, FeedStatus feedStatus);

    @Query("""
SELECT feed FROM Feed feed
WHERE feed.status = 'PUBLISHED'
ORDER BY feed.id DESC
""")
    List<Feed> findAllPublished();

    List<Feed> findByAuthorId(Long authorId);

    @Query("""
SELECT feed FROM Feed feed
WHERE feed.status = 'PUBLISHED'
AND feed.authorId NOT IN :blockedIds
AND (:cursor IS NULL OR feed.id < :cursor)
ORDER BY feed.id DESC
""")
    List<Feed> findFeedsForHome(@Param("cursor") Long cursor, Pageable pageable);
}
