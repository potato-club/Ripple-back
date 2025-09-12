package org.example.rippleback.features.user.infra;

import org.example.rippleback.features.user.domain.Follow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    // ===== 조인 없는 파생 쿼리 =====
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Transactional
    @Modifying
    int deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowingId(Long userId);  // 팔로워 수
    long countByFollowerId(Long userId);   // 팔로잉 수

    // ===== 목록 조회: 조건은 FK 값으로, 필요한 연관만 페치 =====
    @EntityGraph(attributePaths = "follower")
    @Query("""
        select f from Follow f
        where f.followingId = :userId
          and (:cursorId is null or f.id < :cursorId)
        order by f.id desc
    """)
    List<Follow> findFollowers(@Param("userId") Long userId,
                               @Param("cursorId") Long cursorId,
                               Pageable pageable);

    @EntityGraph(attributePaths = "following")
    @Query("""
        select f from Follow f
        where f.followerId = :userId
          and (:cursorId is null or f.id < :cursorId)
        order by f.id desc
    """)
    List<Follow> findFollowings(@Param("userId") Long userId,
                                @Param("cursorId") Long cursorId,
                                Pageable pageable);

    // ===== 기존 서비스 호환용(default 위임) =====
    default boolean existsLink(Long followerId, Long followingId) {
        return existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Transactional
    default int deleteLink(Long followerId, Long followingId) {
        return deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Deprecated // 서비스에서 교체 권장: countByFollowingId/countByFollowerId 사용
    @Query("select count(f) from Follow f where f.followingId = :userId")
    long countFollowers(@Param("userId") Long userId);

    @Deprecated // 서비스에서 교체 권장: countByFollowerId 사용
    @Query("select count(f) from Follow f where f.followerId = :userId")
    long countFollowings(@Param("userId") Long userId);
}
