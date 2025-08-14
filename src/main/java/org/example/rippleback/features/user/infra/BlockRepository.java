package org.example.rippleback.features.user.infra;

import org.example.rippleback.features.user.domain.Block;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {

    @Query("""
        select (count(b) > 0)
        from Block b
        where b.blocker.id = :meId
          and b.blocked.id = :targetId
        """)
    boolean existsMeBlockedTarget(@Param("meId") Long meId,
                                  @Param("targetId") Long targetId);

    @Query("""
        select (count(b) > 0)
        from Block b
        where b.blocker.id = :targetId
          and b.blocked.id = :meId
        """)
    boolean existsTargetBlockedMe(@Param("meId") Long meId,
                                  @Param("targetId") Long targetId);

    @Transactional
    @Modifying
    @Query("""
        delete from Block b
        where b.blocker.id = :meId
          and b.blocked.id = :targetId
        """)
    int deleteLink(@Param("meId") Long meId,
                   @Param("targetId") Long targetId);

    @Query("""
        select b
        from Block b
        join fetch b.blocked bu
        where b.blocker.id = :meId
          and (:cursorId is null or b.id < :cursorId)
        order by b.id desc
        """)
    List<Block> findBlocks(@Param("meId") Long meId,
                           @Param("cursorId") Long cursorId,
                           Pageable pageable);
}