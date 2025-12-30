package org.example.rippleback.features.media.infra;

import org.example.rippleback.features.media.domain.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    List<Media> findByOwnerIdOrderByIdDesc(Long userId);
    Optional<Media> findByIdAndOwnerId(Long id, Long userId);
    boolean existsByIdAndOwnerId(Long id, Long userId);
}
