package org.example.rippleback.features.post.infra;

import org.example.rippleback.features.post.domain.Post;
import org.example.rippleback.features.post.domain.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    long countByAuthorIdAndStatus(Long authorId, PostStatus status); // 게시한(PUBLISHED) 글 개수

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE posts SET comment_count = comment_count + 1 WHERE id = :postId", nativeQuery = true)
    int incrementComment(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE posts SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = :postId", nativeQuery = true)
    int decrementComment(@Param("postId") Long postId);
}