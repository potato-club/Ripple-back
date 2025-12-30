package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.FeedMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedMediaRepository extends JpaRepository<FeedMedia, Long> {

    @Query("""
                select fm
                from FeedMedia fm
                join fetch fm.media m
                where fm.feedId = :feedId
                order by fm.sortOrder asc
            """)
    List<FeedMedia> findWithMediaByFeedId(@Param("feedId") Long feedId);


    @Query("""
                select fm
                from FeedMedia fm
                join fetch fm.media m
                where fm.feedId in :feedIds
                order by fm.feedId asc, fm.sortOrder asc
            """)
    List<FeedMedia> findWithMediaByFeedIdIn(@Param("feedIds") List<Long> feedIds);
}
