package org.example.rippleback.features.user.infra;

import org.example.rippleback.features.user.api.dto.PageCursorResponse;
import org.example.rippleback.features.user.api.dto.UserSummaryDto;
import org.example.rippleback.features.user.domain.UserFollow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    long countByFollowingId(Long userId); // 팔로워 수(나를 팔로우)

    long countByFollowerId(Long userId);  // 팔로잉 수(내가 팔로우)

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Modifying
    @Query("delete from UserFollow f where f.followerId = :followerId and f.followingId = :followingId")
    void deleteLink(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    List<UserFollow> findByFollowingIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable page); // followers

    List<UserFollow> findByFollowerIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable page);  // followings

    List<UserFollow> findByFollowingIdOrderByIdDesc(Long userId, Pageable page);

    List<UserFollow> findByFollowerIdOrderByIdDesc(Long userId, Pageable page);

    // userId 로 받은 사용자와 차단관계가 존재하지 않는 사용자만 목록에 추가
    @EntityGraph(attributePaths = {"follower", "follower.profileMedia"})
    @Query("""
            select f
            from UserFollow f
            where f.followingId = :userId
              and (:cursorId is null or f.id < :cursorId)
              and not exists (
                  select 1
                  from UserBlock b
                  where (b.blockerId = :viewerId and b.blockedId = f.followerId)
                     or (b.blockerId = f.followerId and b.blockedId = :viewerId)
              )
            order by f.id desc
            """)
    List<UserFollow> findFollowersVisibleTo(
            @Param("userId") Long userId,
            @Param("viewerId") Long viewerId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
