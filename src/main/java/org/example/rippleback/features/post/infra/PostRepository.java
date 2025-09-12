package org.example.rippleback.features.post.infra;

import org.example.rippleback.features.post.domain.Post;
import org.example.rippleback.features.post.domain.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    long countByAuthorIdAndStatus(Long authorId, PostStatus status); // 게시한(PUBLISHED) 글 개수
}