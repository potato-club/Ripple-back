package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.FeedTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedTagRepository extends JpaRepository<FeedTag, Long> {
    Optional<FeedTag> findByName(String name);

    @Query("SELECT t FROM FeedTag t WHERE t.name LIKE %:keyword%")
    List<FeedTag> searchByKeyword(@Param("keyword") String keyword);

}