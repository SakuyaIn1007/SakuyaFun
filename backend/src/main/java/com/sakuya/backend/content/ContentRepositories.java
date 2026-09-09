package com.sakuya.backend.content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 内容库内部 Repository 集合保持包内可见，Controller 只能通过 Service 访问持久化细节。 */
interface ContentVolumeRepository extends JpaRepository<ContentVolume, String> {
    List<ContentVolume> findByBookIdOrderByDisplayOrderAsc(String bookId);
    void deleteByBookId(String bookId);
}
interface ContentChapterRepository extends JpaRepository<ContentChapter, String> {
    List<ContentChapter> findByBookIdOrderByDisplayOrderAsc(String bookId);
    Optional<ContentChapter> findByBookIdAndSourceChapterId(String bookId, String sourceChapterId);
    Optional<ContentChapter> findFirstBySourceChapterIdOrderByIdAsc(String sourceChapterId);
    void deleteByBookId(String bookId);
}
interface ContentSourceMappingRepository extends JpaRepository<ContentSourceMapping, UUID> {
    Optional<ContentSourceMapping> findByProviderIgnoreCaseAndExternalBookId(String provider, String externalBookId);
}
interface ContentCatalogEntryRepository extends JpaRepository<ContentCatalogEntry, String> {
    List<ContentCatalogEntry> findByFeedOrderByDisplayOrderAsc(String feed);
    void deleteByFeed(String feed);
}
interface ContentImportJobRepository extends JpaRepository<ContentImportJob, UUID> { }
