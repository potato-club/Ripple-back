package org.example.rippleback.features.user.infra;

import org.example.rippleback.features.user.domain.Block;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {

    // ===== 조인 없는 파생 쿼리 =====
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Transactional
    @Modifying
    int deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    // ===== 목록 조회: 조건은 FK 값으로, 필요한 연관만 페치 =====
    @EntityGraph(attributePaths = "blocked")
    @Query("""
        select b from Block b
        where b.blockerId = :meId
          and (:cursorId is null or b.id < :cursorId)
        order by b.id desc
    """)
    List<Block> findBlocks(@Param("meId") Long meId,
                           @Param("cursorId") Long cursorId,
                           Pageable pageable);

    // ===== 기존 서비스 호환용(default 위임) =====
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
