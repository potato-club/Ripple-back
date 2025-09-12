package org.example.rippleback.features.media.infra;

import org.example.rippleback.features.media.domain.MediaType;
import org.example.rippleback.features.media.domain.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {

    // 포스트의 전체 링크(이미지/비디오 섞여있을 수 있음) — 이미지 정렬 먼저 고려
    List<PostMedia> findByPostIdOrderByPositionAscIdAsc(Long postId);

    // 타입별 존재/개수 체크
    boolean existsByPostIdAndMedia_MediaType(Long postId, MediaType mediaType);
    long countByPostIdAndMedia_MediaType(Long postId, MediaType mediaType);

    // 타입별 삭제(비디오 교체, 이미지 일괄 교체 등에 사용)
    void deleteByPostIdAndMedia_MediaType(Long postId, MediaType mediaType);

    // 특정 미디어가 이미 연결돼 있는지
    boolean existsByPostIdAndMediaId(Long postId, Long mediaId);

    // 비디오 1건 가져오기 (있다면)
    Optional<PostMedia> findFirstByPostIdAndMedia_MediaType(Long postId, MediaType mediaType);

    // 전체 링크 제거(포스트 삭제/초기화 등)
    void deleteByPostId(Long postId);

    // 이미지 전용 조회 (정렬 보장)
    List<PostMedia> findByPostIdAndMedia_MediaTypeOrderByPositionAscIdAsc(Long postId, MediaType mediaType);
}
