package com.sakuya.backend.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NovelReleaseRepository extends JpaRepository<NovelRelease, UUID> {
    List<NovelRelease> findAllByOrderByReleaseDateAscVolumeNameAsc();
}
