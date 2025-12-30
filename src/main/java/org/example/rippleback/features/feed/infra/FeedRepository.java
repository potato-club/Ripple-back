package org.example.rippleback.features.feed.infra;

import org.example.rippleback.features.feed.domain.Feed;
import org.example.rippleback.features.feed.domain.FeedStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {

    long countByAuthorIdAndStatus(Long id, FeedStatus feedStatus);

    @EntityGraph(attributePaths = {"author", "author.profileMedia", "thumbnailMedia"})
    List<Feed> findByAuthorId(Long authorId);

    @EntityGraph(attributePaths = {"author", "author.profileMedia", "thumbnailMedia"})
    @Query("""
            SELECT feed FROM Feed feed
            WHERE feed.status = 'PUBLISHED'
            ORDER BY feed.id DESC
            """)
    List<Feed> findAllPublished();

    @EntityGraph(attributePaths = {"author", "author.profileMedia", "thumbnailMedia"})
    @Query("""
            SELECT feed FROM Feed feed
            WHERE feed.status = 'PUBLISHED'
            AND (:ids IS NULL OR feed.id IN :ids)
            ORDER BY feed.id DESC
            """)
    List<Feed> findByIdIn(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"author", "author.profileMedia", "thumbnailMedia"})
    @Query("""
        SELECT feed FROM Feed feed
        WHERE feed.status = 'PUBLISHED'
        AND (:cursor IS NULL OR feed.id < :cursor)
        ORDER BY mod(feed.id * 13, 10007) DESC, feed.id DESC
        """)
    List<Feed> findFeedsForHome(@Param("cursor") Long cursor, Pageable pageable);


    @Query("""
            select distinct f
            from Feed f
            left join fetch f.author a
            left join fetch a.profileMedia apm
            left join fetch f.thumbnailMedia tm
            left join fetch f.feedMedias fm
            left join fetch fm.media m
            where f.id = :feedId
            """)
    Optional<Feed> findFullViewById(@Param("feedId") Long feedId);

    /**
     * (권장) 생성 직후 응답 매핑 시, author/profileMedia/thumbnailMedia를 한 번에 로딩하고 싶을 때 사용
     */
    @EntityGraph(attributePaths = {"author", "author.profileMedia", "thumbnailMedia"})
    Optional<Feed> findViewById(Long id);

    @Modifying
    @Query("""
            update Feed f
            set f.likeCount = f.likeCount + 1
            where f.id = :feedId
            """)
    void incrementLikeCount(@Param("feedId") Long feedId);

    @Modifying
    @Query("""
            update Feed f
            set f.likeCount = f.likeCount - 1
            where f.id = :feedId
            and f.likeCount > 0
            """)
    int decrementLikeCount(@Param("feedId") Long feedId);

    @Modifying
    @Query("""
            update Feed f
            set f.bookmarkCount = f.bookmarkCount + 1
            where f.id = :feedId
            """)
    void incrementBookmarkCount(@Param("feedId") Long feedId);

    @Modifying
    @Query("""
            update Feed f
            set f.bookmarkCount = f.bookmarkCount - 1
            where f.id = :feedId
            and f.bookmarkCount > 0
            """)
    int decrementBookmarkCount(@Param("feedId") Long feedId);

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
