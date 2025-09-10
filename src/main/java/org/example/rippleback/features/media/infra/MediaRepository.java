package org.example.rippleback.features.media.infra;

import org.example.rippleback.features.media.domain.Media;
import org.example.rippleback.features.media.domain.MediaStatus;
import org.example.rippleback.features.media.domain.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByOwnerIdOrderByIdDesc(Long ownerId);
    long countByOwnerIdAndMediaTypeAndMediaStatus(Long ownerId, MediaType type, MediaStatus status);
}