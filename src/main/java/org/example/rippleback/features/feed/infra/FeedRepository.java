package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.domain.FeedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedRepository extends JpaRepository<Feed, Long> {
    long countByAuthorIdAndStatus(Long authorId, FeedStatus status);

    @Modifying(clearAutomatically = true)
    @Query("""
        update Feed f
           set f.commentCount = f.commentCount + 1
         where f.id = :feedId
    """)
    int incrementCommentCount(@Param("feedId") Long feedId);

    @Modifying(clearAutomatically = true)
    @Query("""
                update Feed f
                   set f.commentCount = f.commentCount - 1
                 where f.id = :feedId
                   and f.commentCount > 0
            """)
    int decrementCommentCount(@Param("feedId") Long feedId);
}
