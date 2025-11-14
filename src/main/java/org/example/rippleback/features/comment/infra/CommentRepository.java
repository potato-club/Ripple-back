package org.example.rippleback.features.comment.infra;

import org.example.rippleback.features.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Modifying
    @Query(value = """
                UPDATE comment
                SET like_count = like_count + 1
                WHERE id = ?1
            """, nativeQuery = true)
    int incLikeCount(Long commentId);

    @Modifying
    @Query(value = """
                UPDATE comment
                SET like_count = GREATEST(like_count - 1, 0)
                WHERE id = ?1
            """, nativeQuery = true)
    int decLikeCount(Long commentId);
}
