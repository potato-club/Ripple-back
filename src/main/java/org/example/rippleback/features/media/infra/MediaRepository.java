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

    List<Media> findByOwnerIdOrderByIdDesc(Long ownerId);

    long countByOwnerIdAndMediaTypeAndMediaStatus(Long ownerId, MediaType mediaType, MediaStatus status);

    Optional<Media> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    List<Media> findAllByIdIn(Iterable<Long> ids);
}
