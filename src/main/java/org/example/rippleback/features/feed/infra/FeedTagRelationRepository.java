package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.FeedTagRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedTagRelationRepository extends JpaRepository<FeedTagRelation, Long> {

    List<FeedTagRelation> findByFeedId(Long feedId);

    @Query("SELECT r.feedId FROM FeedTagRelation r WHERE r.tagId = :tagId")
    List<Long> findFeedIdsByTagId(@Param("tagId") Long tagId);

}
