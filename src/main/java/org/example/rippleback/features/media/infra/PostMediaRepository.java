package org.example.rippleback.features.media.infra;

import org.example.rippleback.features.media.domain.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
    List<PostMedia> findByPostIdOrderByPositionAscIdAsc(Long postId);
    long countByPostId(Long postId);
    void deleteByPostId(Long postId);
}