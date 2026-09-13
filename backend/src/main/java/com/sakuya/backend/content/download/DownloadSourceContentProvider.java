package com.sakuya.backend.content.download;

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

    /** 单次导入内的下载缓存，避免 volumes 与 chapterContent 重复下载同一本。 */
    private final Map<String, String> downloadCache = new ConcurrentHashMap<>();

    public DownloadSourceContentProvider(DownloadSourceClient client) { this.client = client; }

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

    /** 元数据以本地库为准，这里只提供最简结构，避免为导入而多发一次上游请求。 */
    @Override
    public ProviderBook book(String externalBookId) {
        return new ProviderBook(externalBookId, "", "", "", "", List.of(), false);
    }

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

    private String cachedText(String externalBookId) {
        return downloadCache.computeIfAbsent(externalBookId,
            id -> new String(client.download(aidOf(id)), StandardCharsets.UTF_8));
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
}
