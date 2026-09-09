package com.sakuya.backend.content;

import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.catalog.UserLibraryRepository;
import com.sakuya.backend.notification.NotificationService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ContentImportService.java
 * 职责说明：把任意 ContentProvider 的作品转换为统一书目、卷章节和存储对象。
 * 执行流程：完整读取并写入带 revision 的新对象 -> 数据库事务替换目录 -> 最后发布新版本；
 * 任一章节失败时事务回滚，旧目录和旧对象引用继续可读。
 */
@Service
public class ContentImportService {
    private final BookRepository books;
    private final ContentSourceMappingRepository mappings;
    private final ContentVolumeRepository volumes;
    private final ContentChapterRepository chapters;
    private final ContentCatalogEntryRepository catalog;
    private final ContentStorage storage;
    private final UserLibraryRepository userLibrary;
    private final NotificationService notificationService;

    public ContentImportService(BookRepository books, ContentSourceMappingRepository mappings,
            ContentVolumeRepository volumes, ContentChapterRepository chapters,
            ContentCatalogEntryRepository catalog, ContentStorage storage, UserLibraryRepository userLibrary,
            NotificationService notificationService) {
        this.books = books; this.mappings = mappings; this.volumes = volumes; this.chapters = chapters;
        this.catalog = catalog; this.storage = storage;
        this.userLibrary = userLibrary; this.notificationService = notificationService;
    }

    @Transactional
    public String importBook(ContentProvider provider, String externalBookId) {
        ContentProvider.ProviderBook source = provider.book(externalBookId);
        if (source.title() == null || source.title().isBlank()) throw new BusinessException(503, "来源书目缺少标题");
        String bookId = resolveBookId(provider.id(), externalBookId);
        Set<String> previousChapterIds = new HashSet<>(chapters.findByBookIdOrderByDisplayOrderAsc(bookId).stream().map(ContentChapter::getId).toList());
        boolean wasPublished = !previousChapterIds.isEmpty();
        Book book = books.findById(bookId).orElseGet(() -> new Book(bookId, source.title(), source.author(), "", 0f,
            source.tags(), source.description(), null));
        book.updateCatalogMetadata(source.title(), source.author(), "", 0f, source.tags(), source.description(), source.status());

        List<ContentProvider.ProviderVolume> sourceVolumes = provider.volumes(externalBookId);
        if (sourceVolumes.isEmpty()) throw new BusinessException(503, "来源目录为空");
        String revision = UUID.randomUUID().toString();
        Instant now = Instant.now();
        List<ContentVolume> nextVolumes = new ArrayList<>();
        List<ContentChapter> nextChapters = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        int volumeOrder = 0;
        int chapterOrder = 0;
        for (ContentProvider.ProviderVolume sourceVolume : sourceVolumes) {
            String volumeId = stableId("volume", provider.id(), externalBookId, sourceVolume.externalVolumeId());
            nextVolumes.add(new ContentVolume(volumeId, bookId, sourceVolume.title(), volumeOrder++));
            for (ContentProvider.ProviderChapter sourceChapter : sourceVolume.chapters()) {
                String text = provider.chapterContent(externalBookId, sourceChapter.externalChapterId());
                if (text == null || text.isBlank()) throw new BusinessException(503, "章节正文为空：" + sourceChapter.title());
                int offset = fullText.length();
                fullText.append(sourceVolume.title()).append(' ').append(sourceChapter.title()).append('\n').append(text.trim()).append("\n\n");
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                String chapterId = stableId("chapter", provider.id(), externalBookId, sourceChapter.externalChapterId());
                String objectKey = "books/" + bookId + "/revisions/" + revision + "/chapters/" + chapterId + ".txt";
                ContentStorage.StoredObject stored = storage.put(objectKey, bytes, "text/plain;charset=UTF-8");
                nextChapters.add(new ContentChapter(chapterId, bookId, volumeId, sourceChapter.externalChapterId(),
                    sourceChapter.title(), chapterOrder++, stored.key(), stored.sha256(), stored.byteSize(), offset, now));
            }
        }
        byte[] fullBytes = fullText.toString().getBytes(StandardCharsets.UTF_8);
        ContentStorage.StoredObject full = storage.put("books/" + bookId + "/revisions/" + revision + "/full.txt",
            fullBytes, "text/plain;charset=UTF-8");
        ContentStorage.StoredObject cover = storeCover(provider, externalBookId, bookId, revision);
        String rights = source.copyrightRestricted() ? "RESTRICTED" : "AUTHORIZED";

        volumes.deleteByBookId(bookId);
        chapters.deleteByBookId(bookId);
        books.save(book);
        volumes.saveAll(nextVolumes);
        chapters.saveAll(nextChapters);
        book.updateContentPublication(!source.copyrightRestricted(), rights,
            source.copyrightRestricted() ? "来源标记为版权受限，正文未发布" : "由后台 Provider 导入",
            full.key(), cover == null ? book.getCoverObjectKey() : cover.key());
        book.updateFullContentObject(full.key(), full.sha256(), full.byteSize());
        if (cover != null) book.updateCoverObject(cover.key(), cover.sha256(), cover.byteSize());
        books.save(book);
        mappings.findByProviderIgnoreCaseAndExternalBookId(provider.id(), externalBookId)
            .orElseGet(() -> mappings.save(new ContentSourceMapping(provider.id(), externalBookId, bookId)))
            .markSynced(now);
        if(wasPublished){
            nextChapters.stream().filter(value->!previousChapterIds.contains(value.getId())).reduce((first,second)->second).ifPresent(latest->{
                userLibrary.findByBookId(bookId).forEach(item->notificationService.createChapterUpdate(item.getUserId(),bookId,latest.getId(),source.title(),latest.getTitle()));
            });
        }
        return bookId;
    }

    @Transactional
    public void replaceCatalog(Map<String, List<String>> feeds) {
        Instant now = Instant.now();
        feeds.forEach((feed, bookIds) -> {
            catalog.deleteByFeed(feed);
            int order = 0;
            for (String bookId : bookIds) catalog.save(new ContentCatalogEntry(feed, bookId, order++, now));
        });
    }

    private String resolveBookId(String provider, String externalId) {
        return mappings.findByProviderIgnoreCaseAndExternalBookId(provider, externalId)
            .map(ContentSourceMapping::getBookId)
            .orElseGet(() -> "WENKU8".equalsIgnoreCase(provider) ? "wenku8-" + externalId : "content-" + UUID.randomUUID());
    }
    private ContentStorage.StoredObject storeCover(ContentProvider provider, String externalId, String bookId, String revision) {
        try {
            byte[] cover = provider.cover(externalId);
            if (cover == null || cover.length == 0) return null;
            return storage.put("books/" + bookId + "/revisions/" + revision + "/cover.jpg", cover, "image/jpeg");
        } catch (Exception ignored) { return null; }
    }
    private String stableId(String kind, String provider, String bookId, String externalId) {
        String seed = provider + ":" + bookId + ":" + externalId;
        return kind + "-" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
