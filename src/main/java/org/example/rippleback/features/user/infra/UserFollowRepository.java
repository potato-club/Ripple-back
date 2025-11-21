package org.example.rippleback.features.user.infra;

import org.example.rippleback.features.user.domain.UserFollow;
import org.springframework.data.domain.Pageable;
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
}
