package com.sakuya.backend.content;

import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import com.sakuya.backend.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ContentAdminService.java
 * 职责说明：承接人工书目维护、来源绑定、栏目排序和 TXT/EPUB 入库。
 * 执行流程：管理接口完成鉴权与参数校验 -> 本服务写新对象 revision -> 原子替换目录并更新发布状态。
 */
@Service
public class ContentAdminService {
    private final BookRepository books;
    private final ContentSourceMappingRepository mappings;
    private final ContentCatalogEntryRepository catalog;
    private final ContentVolumeRepository volumes;
    private final ContentChapterRepository chapters;
    private final ContentStorage storage;

    public ContentAdminService(BookRepository books, ContentSourceMappingRepository mappings,
            ContentCatalogEntryRepository catalog, ContentVolumeRepository volumes,
            ContentChapterRepository chapters, ContentStorage storage) {
        this.books = books; this.mappings = mappings; this.catalog = catalog;
        this.volumes = volumes; this.chapters = chapters; this.storage = storage;
    }

    @Transactional
    public Book saveBook(String requestedId, String title, String author, String publisher, float rating,
            List<String> tags, String description, String status, String rightsStatus, String licenseNote, boolean published) {
        String id = requestedId == null || requestedId.isBlank() ? "content-" + UUID.randomUUID() : requestedId;
        Book book = books.findById(id).orElseGet(() -> new Book(id, title, author, publisher, rating, tags, description, null));
        book.updateCatalogMetadata(title, author, publisher, rating, tags, description, status);
        // 发布只要求正文已入库；rights_status 已退化为展示字段，不再作为发布前置条件。
        if (published && book.getFullContentObjectKey() == null) {
            throw new BusinessException(400, "只有已上传正文的书籍才能发布");
        }
        book.updateContentPublication(published, rightsStatus, licenseNote, book.getFullContentObjectKey(), book.getCoverObjectKey());
        return books.save(book);
    }

    @Transactional
    public void bindSource(String provider, String externalBookId, String bookId) {
        if (!books.existsById(bookId)) throw new BusinessException(404, "目标书籍不存在");
        var existing = mappings.findByProviderIgnoreCaseAndExternalBookId(provider, externalBookId);
        if (existing.isPresent() && !existing.get().getBookId().equals(bookId)) throw new BusinessException(409, "该来源 ID 已绑定其他书籍");
        if (existing.isEmpty()) mappings.save(new ContentSourceMapping(provider.toUpperCase(), externalBookId, bookId));
    }

    @Transactional
    public void replaceFeed(String feed, List<String> bookIds) {
        String normalized = feed.toUpperCase();
        if (!List.of("RECOMMEND", "NOVELS", "RANKING").contains(normalized)) throw new BusinessException(400, "不支持的栏目");
        for (String bookId : bookIds) if (!books.existsById(bookId)) throw new BusinessException(404, "栏目书籍不存在：" + bookId);
        catalog.deleteByFeed(normalized);
        int order = 0; Instant now = Instant.now();
        for (String bookId : bookIds) catalog.save(new ContentCatalogEntry(normalized, bookId, order++, now));
    }

    @Transactional
    public void uploadCover(String bookId, byte[] bytes, String contentType) {
        Book book = book(bookId);
        String extension = contentType != null && contentType.contains("png") ? ".png" : contentType != null && contentType.contains("webp") ? ".webp" : ".jpg";
        String key = "books/" + bookId + "/manual/cover-" + UUID.randomUUID() + extension;
        var stored = storage.put(key, bytes, contentType == null ? "image/jpeg" : contentType);
        book.updateContentPublication(book.isPublished(), book.getRightsStatus(), book.getLicenseNote(), book.getFullContentObjectKey(), key);
        book.updateCoverObject(stored.key(), stored.sha256(), stored.byteSize());
        books.save(book);
    }

    @Transactional
    public void uploadDocument(String bookId, String fileName, byte[] bytes) {
        Book book = book(bookId);
        List<ParsedChapter> parsed = fileName != null && fileName.toLowerCase().endsWith(".epub") ? parseEpub(bytes) : parseTxt(book.getTitle(), bytes);
        if (parsed.isEmpty()) throw new BusinessException(400, "文档中没有可读取正文");
        String revision = UUID.randomUUID().toString();
        Instant now = Instant.now();
        String volumeId = stableId("volume", bookId, "manual");
        List<ContentChapter> next = new ArrayList<>();
        StringBuilder full = new StringBuilder();
        int order = 0;
        for (ParsedChapter item : parsed) {
            int offset = full.length();
            full.append("正文 ").append(item.title()).append('\n').append(item.content()).append("\n\n");
            String chapterId = stableId("chapter", bookId, String.valueOf(order));
            byte[] content = item.content().getBytes(StandardCharsets.UTF_8);
            var stored = storage.put("books/" + bookId + "/revisions/" + revision + "/chapters/" + chapterId + ".txt", content, "text/plain;charset=UTF-8");
            next.add(new ContentChapter(chapterId, bookId, volumeId, String.valueOf(order), item.title(), order,
                stored.key(), stored.sha256(), stored.byteSize(), offset, now));
            order++;
        }
        var fullStored = storage.put("books/" + bookId + "/revisions/" + revision + "/full.txt",
            full.toString().getBytes(StandardCharsets.UTF_8), "text/plain;charset=UTF-8");
        volumes.deleteByBookId(bookId); chapters.deleteByBookId(bookId);
        volumes.save(new ContentVolume(volumeId, bookId, "正文", 0)); chapters.saveAll(next);
        book.updateContentPublication(false, book.getRightsStatus(), book.getLicenseNote(), fullStored.key(), book.getCoverObjectKey());
        book.updateFullContentObject(fullStored.key(), fullStored.sha256(), fullStored.byteSize());
        books.save(book);
    }

    private List<ParsedChapter> parseTxt(String title, byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8).trim();
        return text.isBlank() ? List.of() : List.of(new ParsedChapter(title, text));
    }
    private List<ParsedChapter> parseEpub(byte[] bytes) {
        Map<String, String> documents = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                if (!entry.isDirectory() && (name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm"))) {
                    documents.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (Exception error) { throw new BusinessException(400, "EPUB 文件无法解析"); }
        return documents.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> {
            String html = entry.getValue();
            String title = capture(html, "(?is)<title[^>]*>(.*?)</title>");
            if (title.isBlank()) title = capture(html, "(?is)<h[1-3][^>]*>(.*?)</h[1-3]>");
            if (title.isBlank()) title = entry.getKey();
            String text = html.replaceAll("(?is)<script.*?</script>|<style.*?</style>", " ")
                .replaceAll("(?s)<[^>]+>", " ").replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replaceAll("[\\t ]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n\n").trim();
            return new ParsedChapter(stripTags(title).trim(), text);
        }).filter(chapter -> !chapter.content().isBlank()).toList();
    }
    private String capture(String value, String regex) {
        var matcher = java.util.regex.Pattern.compile(regex).matcher(value); return matcher.find() ? stripTags(matcher.group(1)) : "";
    }
    private String stripTags(String value) { return value.replaceAll("(?s)<[^>]+>", " ").replace("&nbsp;", " ").trim(); }
    private String stableId(String kind, String bookId, String source) {
        return kind + "-" + UUID.nameUUIDFromBytes((bookId + ":" + source).getBytes(StandardCharsets.UTF_8));
    }
    private Book book(String id) { return books.findById(id).orElseThrow(() -> new BusinessException(404, "图书不存在")); }
    private record ParsedChapter(String title, String content) { }
}
