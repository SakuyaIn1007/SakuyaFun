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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ContentImportService.class);
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

        // ── 取回阶段：先把全部正文读进内存，此阶段不产生任何存储副作用 ──────────────
        // 存储无法参与数据库事务，一旦边读边写，任一章失败回滚后这些对象就成了无人引用的垃圾。
        // 实测 wenku8-3020 因「插图」章节反复中断，留下了 5 份半成品 revision。
        StringBuilder fullText = new StringBuilder();
        List<FetchedChapter> fetched = new ArrayList<>();
        int skippedChapters = 0;
        for (ContentProvider.ProviderVolume sourceVolume : sourceVolumes) {
            String volumeId = stableId("volume", provider.id(), externalBookId, sourceVolume.externalVolumeId());
            int chapterOrder = 0;
            for (ContentProvider.ProviderChapter sourceChapter : sourceVolume.chapters()) {
                String text = provider.chapterContent(externalBookId, sourceChapter.externalChapterId());
                // 「插图」类章节只有图片链接、没有正文。此处若抛异常会让整本回滚，
                // 使该 bookId 恒为 404：用户每次打开都重新整本下载，最终留下 published=true 却零章节的空壳书。
                // 因此跳过空章节而保留其余正文，与 DownloadSourceContentProvider 的过滤口径保持一致。
                if (text == null || text.isBlank()) { skippedChapters++; continue; }
                int offset = fullText.length();
                fullText.append(sourceVolume.title()).append(' ').append(sourceChapter.title()).append('\n').append(text.trim()).append("\n\n");
                fetched.add(new FetchedChapter(volumeId, sourceVolume.title(), sourceChapter, text, chapterOrder++, offset));
            }
        }
        // 全部章节都无正文说明上游整体异常；此时若照常发布，等于又造一本零章节空壳书。
        if (fetched.isEmpty()) throw new BusinessException(503, "来源目录没有可入库的正文");
        if (skippedChapters > 0) {
            log.warn("导入跳过无正文章节 provider={} externalId={} 跳过数={}", provider.id(), externalBookId, skippedChapters);
        }
        byte[] fullBytes = fullText.toString().getBytes(StandardCharsets.UTF_8);
        String fullSha256 = ContentHashes.sha256(fullBytes);

        // ── 幂等短路：正文未变化时不重写存储与目录 ────────────────────────────────
        // 定时任务每天对全部作品重跑导入，若每次新建 revision 全量重写，存储会持续膨胀；
        // 而这份新对象与当前内容完全一致，纯属浪费。
        if (fullSha256.equals(book.getFullContentSha256())) {
            log.debug("正文未变化，跳过重复导入 provider={} externalId={}", provider.id(), externalBookId);
            // 正文不变但书目资料可能已变（标题、作者、标签），仍要落库，只跳过重写正文。
            books.save(book);
            mappings.findByProviderIgnoreCaseAndExternalBookId(bookNamespace(provider.id()), externalBookId)
                .orElseGet(() -> mappings.save(new ContentSourceMapping(bookNamespace(provider.id()), externalBookId, bookId)))
                .markSynced(Instant.now());
            // 正文未变化即没有新章节，无需发送章节更新通知。
            return bookId;
        }

        // ── 落盘阶段：内容已确认有变化，开始写新 revision ──────────────────────────
        String revision = UUID.randomUUID().toString();
        Instant now = Instant.now();
        List<ContentVolume> nextVolumes = new ArrayList<>();
        List<ContentChapter> nextChapters = new ArrayList<>();
        Set<String> addedVolumeIds = new HashSet<>();
        for (FetchedChapter item : fetched) {
            if (addedVolumeIds.add(item.volumeId())) {
                nextVolumes.add(new ContentVolume(item.volumeId(), bookId, item.volumeTitle(), nextVolumes.size()));
            }
            byte[] bytes = item.text().getBytes(StandardCharsets.UTF_8);
            String chapterId = stableId("chapter", provider.id(), externalBookId, item.chapter().externalChapterId());
            String objectKey = "books/" + bookId + "/revisions/" + revision + "/chapters/" + chapterId + ".txt";
            ContentStorage.StoredObject stored = storage.put(objectKey, bytes, "text/plain;charset=UTF-8");
            nextChapters.add(new ContentChapter(chapterId, bookId, item.volumeId(), item.chapter().externalChapterId(),
                item.chapter().title(), item.order(), stored.key(), stored.sha256(), stored.byteSize(), item.offset(), now));
        }
        ContentStorage.StoredObject full = storage.put("books/" + bookId + "/revisions/" + revision + "/full.txt",
            fullBytes, "text/plain;charset=UTF-8");
        ContentStorage.StoredObject cover = storeCover(provider, externalBookId, bookId, revision);
        String rights = source.copyrightRestricted() ? "RESTRICTED" : "AUTHORIZED";

        volumes.deleteByBookId(bookId);
        chapters.deleteByBookId(bookId);
        books.save(book);
        volumes.saveAll(nextVolumes);
        chapters.saveAll(nextChapters);
        // 导入即发布：正文与封面都已完整入库。上游的版权标记只记录到 rights_status 供展示，
        // 不再顺带把作品下架——那会让已入库的正文读不出来。
        book.updateContentPublication(true, rights,
            source.copyrightRestricted() ? "来源标记为版权受限，仅供内部验证" : "由后台 Provider 导入",
            full.key(), cover == null ? book.getCoverObjectKey() : cover.key());
        book.updateFullContentObject(full.key(), full.sha256(), full.byteSize());
        if (cover != null) book.updateCoverObject(cover.key(), cover.sha256(), cover.byteSize());
        books.save(book);
        // 与 resolveBookId 使用同一命名空间写入，否则同一作品会因取数通道不同留下两条映射行。
        String namespace = bookNamespace(provider.id());
        mappings.findByProviderIgnoreCaseAndExternalBookId(namespace, externalBookId)
            .orElseGet(() -> mappings.save(new ContentSourceMapping(namespace, externalBookId, bookId)))
            .markSynced(now);
        notifyChapterUpdates(bookId, source.title(), wasPublished ? previousChapterIds : Set.of(), nextChapters);
        return bookId;
    }

    /**
     * 只对本次新出现的章节发一次更新通知，取最后一章作为「最新更新」。
     * 首次导入（此前无章节）不发通知，否则新增作品会给全部收藏用户推送一次。
     */
    private void notifyChapterUpdates(String bookId, String bookTitle, Set<String> previousChapterIds, List<ContentChapter> chapters) {
        if (previousChapterIds.isEmpty() || chapters.isEmpty()) return;
        chapters.stream()
            .filter(chapter -> !previousChapterIds.contains(chapter.getId()))
            .reduce((first, second) -> second)
            .ifPresent(latest -> userLibrary.findByBookId(bookId).forEach(item ->
                notificationService.createChapterUpdate(item.getUserId(), bookId, latest.getId(), bookTitle, latest.getTitle())));
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

    /**
     * 同一部作品的不同取数通道必须解析到同一个 bookId。
     * 若按 provider 原值分别解析，WENKU8_CDN 不满足 "WENKU8".equals 判断，
     * 会落到 content-{UUID} 分支另建一本新书：原 bookId 依旧 404，
     * 于是每次读取都重新整本下载并回填到一个新的孤儿书上。
     */
    private String resolveBookId(String provider, String externalId) {
        String namespace = bookNamespace(provider);
        return mappings.findByProviderIgnoreCaseAndExternalBookId(namespace, externalId)
            .map(ContentSourceMapping::getBookId)
            .orElseGet(() -> "WENKU8".equalsIgnoreCase(namespace) ? "wenku8-" + externalId : "content-" + UUID.randomUUID());
    }

    /** 把同一作品的不同取数通道归一到同一命名空间，保证 bookId 稳定且唯一。 */
    private static String bookNamespace(String provider) {
        return "WENKU8_CDN".equalsIgnoreCase(provider) ? "WENKU8" : provider;
    }
    private ContentStorage.StoredObject storeCover(ContentProvider provider, String externalId, String bookId, String revision) {
        try {
            byte[] cover = provider.cover(externalId);
            if (cover == null || cover.length == 0) return null;
            return storage.put("books/" + bookId + "/revisions/" + revision + "/cover.jpg", cover, "image/jpeg");
        } catch (Exception ignored) { return null; }
    }
    /** 取回阶段暂存的一章：正文、所属卷与全文偏移，落盘阶段据此生成实体与存储对象。 */
    private record FetchedChapter(String volumeId, String volumeTitle, ContentProvider.ProviderChapter chapter,
                                  String text, int order, int offset) { }

    private String stableId(String kind, String provider, String bookId, String externalId) {
        String seed = provider + ":" + bookId + ":" + externalId;
        return kind + "-" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
