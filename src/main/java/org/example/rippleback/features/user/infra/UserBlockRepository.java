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

    @Transactional
    @Modifying
    int deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @EntityGraph(attributePaths = "blocked")
    @Query("select b from UserBlock b " +
            "where b.blockerId = :meId " +
            "  and (:cursorId is null or b.id < :cursorId) " +
            "order by b.id desc")
    List<UserBlock> findBlocks(@Param("meId") Long meId,
                               @Param("cursorId") Long cursorId,
                               Pageable pageable);


    default boolean existsMeBlockedTarget(Long meId, Long targetId) {
        return existsByBlockerIdAndBlockedId(meId, targetId);
    }

    default boolean existsTargetBlockedMe(Long meId, Long targetId) {
        return existsByBlockerIdAndBlockedId(targetId, meId);
    }

    @Transactional
    default int deleteLink(Long meId, Long targetId) {
        return deleteByBlockerIdAndBlockedId(meId, targetId);
    }
}
