package org.example.rippleback.features.user.infra;

import org.example.rippleback.features.user.domain.Follow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    @Query("""
            select (count(f) > 0)
            from Follow f
            where f.follower.id = :followerId
              and f.following.id = :followingId
            """)
    boolean existsLink(@Param("followerId") Long followerId,
                       @Param("followingId") Long followingId);

    @Transactional
    @Modifying
    @Query("""
            delete from Follow f
            where f.follower.id = :followerId
              and f.following.id = :followingId
            """)
    int deleteLink(@Param("followerId") Long followerId,
                   @Param("followingId") Long followingId);

    @Query("""
            select f
            from Follow f
            join fetch f.follower fu
            where f.following.id = :userId
              and (:cursorId is null or f.id < :cursorId)
            order by f.id desc
            """)
    List<Follow> findFollowers(@Param("userId") Long userId,
                               @Param("cursorId") Long cursorId,
                               Pageable pageable);

    @Query("""
            select f
            from Follow f
            join fetch f.following tu
            where f.follower.id = :userId
              and (:cursorId is null or f.id < :cursorId)
            order by f.id desc
            """)
    List<Follow> findFollowings(@Param("userId") Long userId,
                                @Param("cursorId") Long cursorId,
                                Pageable pageable);

    @Query("select count(f) from Follow f where f.following.id = :userId")
    long countFollowers(@Param("userId") Long userId);

    @Query("select count(f) from Follow f where f.follower.id = :userId")
    long countFollowings(@Param("userId") Long userId);
}