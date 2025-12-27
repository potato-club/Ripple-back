package org.example.rippleback.features.user.infra;

import org.example.rippleback.features.user.domain.UserBlock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    // 파생 쿼리라 이름 이렇게 지음
    @Transactional
    @Modifying
    int deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @EntityGraph(attributePaths = {"blocked", "blocked.profileMedia"})
    @Query("select b from UserBlock b " +
            "where b.blockerId = :meId " +
            "  and (:cursorId is null or b.id < :cursorId) " +
            "order by b.id desc")
    List<UserBlock> findBlocks(@Param("meId") Long meId,
                               @Param("cursorId") Long cursorId,
                               Pageable pageable);


    @Query("""
                SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END
                FROM UserBlock b
                WHERE (b.blockerId = :userA AND b.blockedId = :userB)
                   OR (b.blockerId = :userB AND b.blockedId = :userA)
            """)
    boolean existsAnyBlock(
            @Param("userA") Long userA,
            @Param("userB") Long userB
    );


    default boolean existsMeBlockedTarget(Long meId, Long targetId) {
        return existsByBlockerIdAndBlockedId(meId, targetId);
    }

    @Transactional
    default int deleteBlockLink(Long meId, Long targetId) {
        return deleteByBlockerIdAndBlockedId(meId, targetId);
    }
}
