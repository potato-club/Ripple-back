package org.example.rippleback.features.comment.infra;

import org.example.rippleback.features.comment.domain.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    @Modifying
    @Query(value = """
                INSERT INTO comment_like (user_id, comment_id, created_at)
                VALUES (?1, ?2, now())
                ON CONFLICT ON CONSTRAINT uq_comment_like_user_comment DO NOTHING
            """, nativeQuery = true)
    int insertIgnore(Long userId, Long commentId);

    @Modifying
    @Query(value = """
                DELETE FROM comment_like
                WHERE user_id = ?1 AND comment_id = ?2
            """, nativeQuery = true)
    int deleteOne(Long userId, Long commentId);

    @Query(value = """
                SELECT EXISTS (
                  SELECT 1 FROM comment_like
                  WHERE user_id = ?1 AND comment_id = ?2
                )
            """, nativeQuery = true)
    boolean existsByUserAndComment(Long userId, Long commentId);

    @Modifying
    @Query(value = """
                DELETE FROM comment_like
                WHERE comment_id = ?1
            """, nativeQuery = true)
    int deleteAllByCommentId(Long commentId);
}
