package com.sakuya.backend.content.download;

import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.catalog.BookRepository;
import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.content.ContentProvider;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * DownloadSourceContentProvider.java
 * 职责说明：把 CDN 整本 TXT 适配为统一的 ContentProvider 卷章协议，供 ContentImportService 落库。
 * 执行流程：下载整本 -> 离线切分 -> 按卷分组为 ProviderVolume -> 按偏移截取章节正文。
 *
 * 与该 provider 并列的 Wenku8ContentProvider 走内部适配器（受 Cloudflare 影响），
 * 本实现直接访问 CDN，不携带凭据，用于「本地无正文」时的按需回填。
 * 下载源没有目录枚举接口，因此 catalog() 无法实现，批量导入仍只能走适配器。
 */
@Component
public class DownloadSourceContentProvider implements ContentProvider {

    private final DownloadSourceClient client;
    private final BookRepository books;

    /**
     * 导入过程中的下载缓存，避免 volumes 与 chapterContent 重复下载同一本。
     *
     * 必须有界：本类是单例 Bean，而整本 TXT 可达 5 MB 以上。若无上限，
     * 遍历 73 本书会累积数百 MB 常驻内存。以容量 + 插入序淘汰最旧条目，
     * 足以覆盖「同一本书的连续导入调用」这一实际访问模式。
     */
    private static final int CACHE_CAPACITY = 4;

    private final Map<String, String> downloadCache = new LinkedHashMap<>(CACHE_CAPACITY, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > CACHE_CAPACITY;
        }
    };

    public DownloadSourceContentProvider(DownloadSourceClient client, BookRepository books) {
        this.client = client;
        this.books = books;
    }

    @Override public String id() { return "WENKU8_CDN"; }

    /** 下载源只能按数字 aid 取单本；13 本短 slug 书没有 aid，必须明确排除。 */
    public static boolean supportsAid(String bookId) {
        if (bookId == null || bookId.isBlank()) return false;
        return aidDigits(bookId).matches("\\d{1,9}");
    }

    private static String aidDigits(String bookId) {
        return bookId.startsWith("wenku8-") ? bookId.substring("wenku8-".length()) : bookId;
    }

    private static int aidOf(String bookId) { return Integer.parseInt(aidDigits(bookId)); }

    @Override
    public List<ProviderCatalogItem> catalog(String mode) {
        throw new UnsupportedOperationException("下载源没有目录枚举接口，无法批量导入；请使用适配器 provider");
    }

    /**
     * 元数据取自本地库：书目信息此前已由目录同步写入，无需再访问上游。
     * 标题必须非空——ContentImportService 对空标题会拒绝导入，因此这里显式兜底。
     */
    @Override
    public ProviderBook book(String externalBookId) {
        Book stored = books.findById("wenku8-" + externalBookId).orElse(null);
        if (stored == null) throw new BusinessException(404, "该书尚未在本地库中登记，无法仅凭下载源导入");
        String title = stored.getTitle() == null ? "" : stored.getTitle();
        if (title.isBlank()) throw new BusinessException(503, "本地书目缺少标题，无法导入");
        return new ProviderBook(externalBookId, title, nullSafe(stored.getAuthor()), nullSafe(stored.getDescription()),
            nullSafe(stored.getStatus()), stored.getTags() == null ? List.of() : List.copyOf(stored.getTags()),
            stored.isCopyrightRestricted());
    }

    private String nullSafe(String value) { return value == null ? "" : value; }

    @Override
    public List<ProviderVolume> volumes(String externalBookId) {
        if (!supportsAid(externalBookId)) throw new BusinessException(400, "该小说没有可用的下载标识");
        String text = cachedText(externalBookId);
        ChapterSplitter.SplitResult result = ChapterSplitter.split(text);
        // 按卷名分组，保持首次出现顺序，使目录顺序与原文一致。
        Map<String, List<ChapterSplitter.SplitChapter>> grouped = new LinkedHashMap<>();
        for (ChapterSplitter.SplitChapter chapter : result.chapters()) {
            grouped.computeIfAbsent(chapter.volumeTitle(), ignored -> new ArrayList<>()).add(chapter);
        }
        List<ProviderVolume> volumes = new ArrayList<>();
        int volumeOrder = 0;
        for (Map.Entry<String, List<ChapterSplitter.SplitChapter>> entry : grouped.entrySet()) {
            List<ProviderChapter> chapters = new ArrayList<>();
            int chapterOrder = 0;
            for (ChapterSplitter.SplitChapter chapter : entry.getValue()) {
                // 「插图」类章节只有图片链接、没有正文，而 importBook 对空正文会中断整本导入，
                // 必须在 provider 层丢弃，否则一本书里出现一张插图就会导致整本无法入库。
                String body = extractBody(text, result, chapter);
                if (hasNoReadableText(body)) continue;
                chapters.add(new ProviderChapter(String.valueOf(chapter.offset()), chapter.title(), chapterOrder++));
            }
            String volumeTitle = entry.getKey().isBlank() ? "" : entry.getKey();
            volumes.add(new ProviderVolume("v" + volumeOrder++, volumeTitle, List.copyOf(chapters)));
        }
        return List.copyOf(volumes);
    }

    /**
     * 章节正文 = 从本章偏移起、到下一章偏移止的文本，去掉首行的「卷名 标题」标题行。
     * 依赖切分结果的偏移，不重新下载，因此同一本书的多章读取只产生一次网络请求。
     */
    @Override
    public String chapterContent(String externalBookId, String externalChapterId) {
        String text = cachedText(externalBookId);
        ChapterSplitter.SplitResult result = ChapterSplitter.split(text);
        int offset = Integer.parseInt(externalChapterId);
        for (ChapterSplitter.SplitChapter chapter : result.chapters()) {
            if (chapter.offset() == offset) return extractBody(text, result, chapter);
        }
        return "";
    }

    @Override
    public byte[] cover(String externalBookId) { return new byte[0]; }

    /** 整本正文与章节锚点，供回退链路在异步入库完成前直接把内容返回给用户。 */
    public record FullTextDocument(String title, String content, List<ChapterAnchor> chapters) { }

    public record ChapterAnchor(String chapterId, String title, String volumeTitle, int offset) { }

    /**
     * 读取整本正文与锚点。与 volumes() 共用同一份下载缓存，因此一次回退只产生一次网络请求。
     * 章节 id 沿用偏移字符串，与 volumes() 产出的 externalChapterId 保持一致。
     */
    public FullTextDocument fullText(String externalBookId) {
        if (!supportsAid(externalBookId)) throw new BusinessException(400, "该小说没有可用的下载标识");
        String text = cachedText(externalBookId);
        ChapterSplitter.SplitResult result = ChapterSplitter.split(text);
        List<ChapterAnchor> anchors = new ArrayList<>();
        for (ChapterSplitter.SplitChapter chapter : result.chapters()) {
            if (hasNoReadableText(extractBody(text, result, chapter))) continue;
            anchors.add(new ChapterAnchor(String.valueOf(chapter.offset()), chapter.title(),
                chapter.volumeTitle(), chapter.offset()));
        }
        String title = books.findById("wenku8-" + externalBookId).map(Book::getTitle).orElse("");
        return new FullTextDocument(title == null ? "" : title, text, List.copyOf(anchors));
    }

    /** LinkedHashMap 非线程安全，而回退链路可能被多个读取请求并发调用，故整体加锁。 */
    private synchronized String cachedText(String externalBookId) {
        String cached = downloadCache.get(externalBookId);
        if (cached != null) return cached;
        String text = new String(client.download(aidOf(externalBookId)), StandardCharsets.UTF_8);
        downloadCache.put(externalBookId, text);
        return text;
    }

    /** 截取某章正文：从本章偏移到下一章偏移，去掉首行的「卷名 标题」标题行。 */
    private String extractBody(String text, ChapterSplitter.SplitResult result, ChapterSplitter.SplitChapter chapter) {
        int end = text.length();
        for (ChapterSplitter.SplitChapter candidate : result.chapters()) {
            if (candidate.offset() > chapter.offset()) { end = candidate.offset(); break; }
        }
        String block = text.substring(chapter.offset(), end);
        int newline = block.indexOf('\n');
        return newline >= 0 ? block.substring(newline + 1).trim() : block.trim();
    }

    /**
     * 判断章节是否没有可阅读文本。
     * 实测《文学少女》每卷都有一个「插图」章节，其内容形如
     * {@code <!--image-->https://pic.example/1.jpg<!--image-->}，只有图片链接而无正文。
     * 这类章节若进入导入流程，会因正文为空而中断整本入库，必须提前丢弃。
     */
    private boolean hasNoReadableText(String body) {
        if (body.isBlank()) return true;
        // 去掉全部图片标记与链接后若再无内容，则视为无可阅读文本。
        String stripped = body.replace("<!--image-->", "")
                              .replaceAll("https?://\\S+", "")
                              .replaceAll("\\s", "");
        return stripped.isEmpty();
    }
}
