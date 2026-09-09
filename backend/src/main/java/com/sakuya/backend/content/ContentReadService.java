package com.sakuya.backend.content;

import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import com.sakuya.backend.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ContentReadService.java
 * 职责说明：作为 Android 唯一内容读取入口，从数据库和 ContentStorage 组装来源无关 DTO。
 * 执行流程：解析稳定或旧来源 ID -> 校验发布与版权 -> 读取结构/对象 -> 校验 SHA-256 -> 返回稳定响应。
 */
@Service
public class ContentReadService {
    private final BookRepository books;
    private final ContentSourceMappingRepository mappings;
    private final ContentVolumeRepository volumes;
    private final ContentChapterRepository chapters;
    private final ContentStorage storage;

    public ContentReadService(BookRepository books, ContentSourceMappingRepository mappings,
            ContentVolumeRepository volumes, ContentChapterRepository chapters, ContentStorage storage) {
        this.books = books; this.mappings = mappings; this.volumes = volumes; this.chapters = chapters; this.storage = storage;
    }

    @Transactional(readOnly = true)
    public NovelPage search(String keyword, int page, int pageSize) {
        String query = keyword == null ? "" : keyword.trim();
        var pageable = PageRequest.of(page, pageSize);
        var result = query.isBlank() ? books.findByPublishedTrue(pageable)
            : books.findByPublishedTrueAndTitleContainingIgnoreCaseOrPublishedTrueAndAuthorContainingIgnoreCase(query, query, pageable);
        return new NovelPage(result.getContent().stream().map(this::novelDto).toList(), result.hasNext() ? page + 1 : null);
    }
    @Transactional(readOnly = true)
    public NovelDto novel(String requestedId) { Book book = resolveBook(requestedId); ensurePublished(book); return novelDto(book); }
    @Transactional(readOnly = true)
    public ChapterIndexDto chapterIndex(String requestedId) {
        Book book = resolveBook(requestedId); ensureReadable(book);
        List<ContentChapter> allChapters = chapters.findByBookIdOrderByDisplayOrderAsc(book.getId());
        List<VolumeDto> result = volumes.findByBookIdOrderByDisplayOrderAsc(book.getId()).stream().map(volume ->
            new VolumeDto(volume.getId(), volume.getTitle(), allChapters.stream()
                .filter(chapter -> chapter.getVolumeId().equals(volume.getId()))
                .map(chapter -> new ChapterDto(chapter.getId(), chapter.getTitle(), chapter.getDisplayOrder())).toList())
        ).toList();
        if (result.isEmpty()) throw new BusinessException(404, "该书目录尚未入库");
        return new ChapterIndexDto(book.getId(), result);
    }
    @Transactional(readOnly = true)
    public ChapterContentDto chapter(String requestedChapterId, String requestedBookId) {
        ContentChapter chapter = chapters.findById(requestedChapterId).orElseGet(() -> {
            // 旧来源章节 ID 可能在不同作品中重复，客户端携带 bookId 时必须限定到对应作品。
            if (requestedBookId != null && !requestedBookId.isBlank()) {
                String resolvedBookId = resolveBook(requestedBookId).getId();
                return chapters.findByBookIdAndSourceChapterId(resolvedBookId, requestedChapterId)
                    .orElseThrow(() -> new BusinessException(404, "章节尚未入库"));
            }
            return chapters.findFirstBySourceChapterIdOrderByIdAsc(requestedChapterId)
                .orElseThrow(() -> new BusinessException(404, "章节尚未入库"));
        });
        if (requestedBookId != null && !requestedBookId.isBlank()) {
            String resolvedBookId = resolveBook(requestedBookId).getId();
            if (!chapter.getBookId().equals(resolvedBookId)) {
                throw new BusinessException(404, "章节不属于指定图书");
            }
        }
        Book book = resolveBook(chapter.getBookId()); ensureReadable(book);
        byte[] content = verifiedRead(chapter.getContentObjectKey(), chapter.getSha256());
        return new ChapterContentDto(book.getId(), chapter.getId(), chapter.getTitle(), new String(content, StandardCharsets.UTF_8));
    }
    @Transactional(readOnly = true)
    public FullContentDto fullContent(String requestedId) {
        Book book = resolveBook(requestedId); ensureReadable(book);
        if (book.getFullContentObjectKey() == null || !storage.exists(book.getFullContentObjectKey())) {
            throw new BusinessException(404, "该书全文尚未入库");
        }
        byte[] fullBytes = verifiedRead(book.getFullContentObjectKey(), book.getFullContentSha256());
        String content = new String(fullBytes, StandardCharsets.UTF_8);
        List<ContentChapter> bookChapters = chapters.findByBookIdOrderByDisplayOrderAsc(book.getId());
        List<ChapterAnchorDto> anchors = new ArrayList<>();
        var volumeNames = volumes.findByBookIdOrderByDisplayOrderAsc(book.getId()).stream()
            .collect(java.util.stream.Collectors.toMap(ContentVolume::getId, ContentVolume::getTitle));
        for (ContentChapter chapter : bookChapters) {
            anchors.add(new ChapterAnchorDto(chapter.getId(), chapter.getTitle(), volumeNames.getOrDefault(chapter.getVolumeId(), ""), chapter.getFullTextOffset()));
        }
        return new FullContentDto(book.getId(), book.getTitle(), content, List.copyOf(anchors));
    }
    @Transactional(readOnly = true)
    public CoverDto cover(String requestedId) {
        Book book = resolveBook(requestedId); ensurePublished(book);
        if (book.getCoverObjectKey() == null || !storage.exists(book.getCoverObjectKey())) throw new BusinessException(404, "封面尚未入库");
        return new CoverDto(verifiedRead(book.getCoverObjectKey(), book.getCoverSha256()), coverContentType(book.getCoverObjectKey()));
    }
    public boolean hasBook(String requestedId) {
        try { resolveBook(requestedId); return true; } catch (BusinessException ignored) { return false; }
    }
    public String resolveBookId(String requestedId) { return resolveBook(requestedId).getId(); }

    private Book resolveBook(String requestedId) {
        if (requestedId == null || requestedId.isBlank()) throw new BusinessException(400, "书籍 ID 不能为空");
        return books.findById(requestedId).or(() -> {
            String external = requestedId.startsWith("wenku8-") ? requestedId.substring("wenku8-".length()) : requestedId;
            return mappings.findByProviderIgnoreCaseAndExternalBookId("WENKU8", external).flatMap(mapping -> books.findById(mapping.getBookId()));
        }).orElseThrow(() -> new BusinessException(404, "图书不存在"));
    }
    private void ensureReadable(Book book) {
        ensurePublished(book);
        if (!"AUTHORIZED".equalsIgnoreCase(book.getRightsStatus())) throw new BusinessException(403, "该内容暂未获得正文分发授权");
    }
    private void ensurePublished(Book book) { if (!book.isPublished()) throw new BusinessException(404, "内容尚未发布"); }
    private NovelDto novelDto(Book book) {
        String coverUrl = book.getCoverObjectKey() == null ? book.getCoverPath() : "/content/novels/" + book.getId() + "/cover";
        return new NovelDto(book.getId(), book.getTitle(), book.getAuthor(), book.getDescription(), book.getStatus(),
            List.copyOf(book.getTags()), book.isCopyrightRestricted(), coverUrl, book.getRightsStatus());
    }
    private byte[] verifiedRead(String key, String expectedHash) {
        byte[] content = storage.read(key);
        if (expectedHash != null && !expectedHash.equals(ContentHashes.sha256(content))) throw new BusinessException(503, "内容文件完整性校验失败");
        return content;
    }
    private String coverContentType(String key) {
        String normalized = key.toLowerCase(java.util.Locale.ROOT);
        if (normalized.endsWith(".png")) return "image/png";
        if (normalized.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    public record NovelPage(List<NovelDto> items, Integer nextPage) { }
    public record NovelDto(String id, String title, String author, String description, String status,
                           List<String> tags, boolean copyright, String coverUrl, String rightsStatus) { }
    public record ChapterIndexDto(String bookId, List<VolumeDto> volumes) { }
    public record VolumeDto(String id, String title, List<ChapterDto> chapters) { }
    public record ChapterDto(String id, String title, int order) { }
    public record ChapterContentDto(String novelId, String chapterId, String title, String content) { }
    public record FullContentDto(String novelId, String title, String content, List<ChapterAnchorDto> chapters) { }
    public record ChapterAnchorDto(String chapterId, String title, String volumeTitle, int offset) { }
    public record CoverDto(byte[] bytes, String contentType) { }
}
