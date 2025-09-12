package org.example.rippleback.features.media.infra;

import org.example.rippleback.features.media.domain.Media;
import org.example.rippleback.features.media.domain.MediaStatus;
import org.example.rippleback.features.media.domain.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    // 소유자별 최신순 조회
    List<Media> findByOwnerIdOrderByIdDesc(Long ownerId);

    // 소유자+타입+상태 카운트 (예: READY만 수 제한)
    long countByOwnerIdAndMediaTypeAndMediaStatus(Long ownerId, MediaType mediaType, MediaStatus status);

    // 소유 확인용
    Optional<Media> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    // 배치 로딩 편의
    List<Media> findAllByIdIn(Iterable<Long> ids);
}
