package com.sakuya.backend.reading;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 阅读同步持久化仅向 ReadingSyncService 暴露，Controller 不直接操作 JPA。 */
interface ReadingProgressStateRepository extends JpaRepository<ReadingProgressState, ReadingProgressState.Id> {
    List<ReadingProgressState> findByIdUserId(UUID userId);
    Optional<ReadingProgressState> findByIdUserIdAndIdBookId(UUID userId, String bookId);
}

interface ReadingBookmarkStateRepository extends JpaRepository<ReadingBookmarkState, ReadingBookmarkState.Id> {
    List<ReadingBookmarkState> findByIdUserId(UUID userId);
    Optional<ReadingBookmarkState> findByIdUserIdAndIdBookmarkId(UUID userId, UUID bookmarkId);
}
