package com.sakuya.backend.content;

import com.sakuya.backend.catalog.NovelDetailBackfillService;
import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.content.download.DownloadSourceContentProvider;
import com.sakuya.backend.wenku8.Wenku8GatewayService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * ContentFallbackFacade.java
 * 职责说明：实施迁移期 DB_FIRST_WITH_ADAPTER_FALLBACK 策略并记录命中指标。
 * 执行流程：优先读取 ContentReadService -> 仅 404 时允许访问旧适配器 -> 异步回填该作品；
 * DB_ONLY 模式下任何缺失都直接返回明确 404。
 */
@Service
public class ContentFallbackFacade {
    /** 单次搜索最多新导入的作品数：整本导入成本高，翻页浏览不该触发几十次连续下载。 */
    private static final int MAX_BACKFILLS_PER_SEARCH = 3;
    private static final Logger log = LoggerFactory.getLogger(ContentFallbackFacade.class);
    private final ContentReadService database;
    private final Wenku8GatewayService gateway;
    private final ContentImportRunner imports;
    private final ContentManagementProperties properties;
    private final MeterRegistry metrics;
    private final DownloadSourceContentProvider downloadSource;
    private final NovelDetailBackfillService detailBackfill;
    public ContentFallbackFacade(ContentReadService database, Wenku8GatewayService gateway, ContentImportRunner imports,
            ContentManagementProperties properties, MeterRegistry metrics, DownloadSourceContentProvider downloadSource,
            NovelDetailBackfillService detailBackfill) {
        this.database = database; this.gateway = gateway; this.imports = imports; this.properties = properties; this.metrics = metrics;
        this.downloadSource = downloadSource; this.detailBackfill = detailBackfill;
    }
    public Object search(String keyword, int page, int pageSize) {
        ContentReadService.NovelPage stored = database.search(keyword, page, pageSize);
        if (!stored.items().isEmpty() || !fallbackEnabled()) { hit("search"); return stored; }
        var upstream = keyword == null || keyword.isBlank() ? gateway.list("lastupdate") : gateway.search(keyword, page);
        List<Map<String, Object>> items = upstream.items().stream().map(this::canonicalNovel).toList();
        backfillMissing(items);
        fallback("search"); return new ContentReadService.NovelPage(items.stream().map(this::novelDto).toList(), upstream.nextPage());
    }
    public Object novel(String bookId) {
        try {
            Object result = database.novel(bookId);
            hit("novel");
            // 本地命中后的**旁路**：若该书从未补全过详情，异步补抓完整简介。
            // 这不是回退分支的一部分——404 回退语义完全不变（下文一字未动）。
            // DB_ONLY 的语义是「不访问上游」，此处据此跳过；补抓本身异步且永不抛异常。
            if (fallbackEnabled()) detailBackfill.schedule(bookId);
            return result;
        }
        catch (BusinessException error) {
            if (error.getCode() != 404 || !fallbackEnabled() || !canFallback(bookId)) throw error;
            String external = externalId(bookId); Map<String, Object> raw = canonicalNovel(gateway.novel(external));
            imports.backfill("WENKU8", external); fallback("novel"); return novelDto(raw);
        }
    }
    public Object chapters(String bookId) {
        try { Object result = database.chapterIndex(bookId); hit("chapters"); return result; }
        catch (BusinessException error) {
            if (error.getCode() != 404 || !fallbackEnabled() || !canFallback(bookId)) throw error;
            String external = externalId(bookId); Map<String, Object> raw = new LinkedHashMap<>(gateway.chapters(external));
            raw.put("bookId", canonicalBookId(external)); imports.backfill("WENKU8", external); fallback("chapters"); return raw;
        }
    }
    public Object chapter(String chapterId, String bookId) {
        try { Object result = database.chapter(chapterId, bookId); hit("chapter"); return result; }
        catch (BusinessException error) {
            if (error.getCode() != 404 || !fallbackEnabled() || bookId == null || bookId.isBlank()) throw error;
            String external = externalId(bookId); Map<String, Object> raw = new LinkedHashMap<>(gateway.content(external, chapterId));
            raw.put("novelId", canonicalBookId(external)); raw.put("chapterId", chapterId);
            imports.backfill("WENKU8", external); fallback("chapter"); return raw;
        }
    }
    public Object fullContent(String bookId) {
        try { Object result = database.fullContent(bookId); hit("full"); return result; }
        catch (BusinessException error) {
            if (error.getCode() != 404 || !fallbackEnabled() || !canFallback(bookId)) throw error;
            // 下载源直连 CDN，不经过 Cloudflare 验证。必须同步返回内容本身：
            // backfill 是 @Async，若只触发它就去读库，异步导入尚未完成，必然再次 404。
            // 异步回填仍然照做，使下次读取直接命中数据库。
            DownloadSourceContentProvider.FullTextDocument document = downloadSourceIfPossible(bookId);
            if (document != null) {
                fallback("full");
                return new ContentReadService.FullContentDto(bookId, document.title(), document.content(),
                    document.chapters().stream().map(anchor -> new ContentReadService.ChapterAnchorDto(
                        anchor.chapterId(), anchor.title(), anchor.volumeTitle(), anchor.offset())).toList());
            }
            String external = externalId(bookId); var raw = gateway.fullContent(external);
            var anchors = raw.chapters().stream().map(anchor -> new ContentReadService.ChapterAnchorDto(
                anchor.chapterId(), anchor.title(), anchor.volumeTitle(), anchor.offset())).toList();
            imports.backfill("WENKU8", external); fallback("full");
            return new ContentReadService.FullContentDto(canonicalBookId(external), raw.title(), raw.content(), anchors);
        }
    }
    public ContentReadService.CoverDto cover(String bookId) {
        try { var result = database.cover(bookId); hit("cover"); return result; }
        catch (BusinessException error) {
            if (error.getCode() != 404 || !fallbackEnabled() || !canFallback(bookId)) throw error;
            String external = externalId(bookId); fallback("cover"); return new ContentReadService.CoverDto(gateway.cover(external), "image/jpeg");
        }
    }
    /**
     * 为搜索结果里尚未入库的作品排队回填。
     *
     * 回填是异步的整本导入，成本很高，因此设两道闸：
     * 已入库的作品只是这次关键词没命中，不需要也不应该重新导入；
     * 单次搜索最多只新增 MAX_BACKFILLS_PER_SEARCH 本，避免翻一页就触发几十次整本下载。
     */
    private void backfillMissing(List<Map<String, Object>> items) {
        int budget = MAX_BACKFILLS_PER_SEARCH;
        for (Map<String, Object> item : items) {
            String externalId = string(item.get("id"));
            if (externalId.isBlank()) continue;
            if (database.hasBook(externalId)) continue;
            if (budget-- <= 0) {
                log.debug("本次搜索回填已达上限 {}，其余作品留待后续读取时按需回填", MAX_BACKFILLS_PER_SEARCH);
                return;
            }
            imports.backfill("WENKU8", externalId);
        }
    }

    /**
     * 尝试用下载源直接取回全文；不可用（无 aid、CDN 缺资源、被拦截等）时返回 null
     * 交由适配器兜底。任何异常都不向上抛，避免单本下载失败中断整条回退链路。
     */
    private DownloadSourceContentProvider.FullTextDocument downloadSourceIfPossible(String bookId) {
        String external = externalId(bookId);
        if (!DownloadSourceContentProvider.supportsAid(external)) return null;
        try {
            DownloadSourceContentProvider.FullTextDocument document = downloadSource.fullText(external);
            // 异步入库，使用户下次读取直接命中数据库；失败只记日志，不影响本次返回。
            imports.backfill("WENKU8_CDN", external);
            return document;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean fallbackEnabled() { return "DB_FIRST_WITH_ADAPTER_FALLBACK".equals(properties.normalizedReadMode()); }
    private void hit(String operation) { metrics.counter("content.read", "result", "db_hit", "operation", operation).increment(); }
    private void fallback(String operation) { metrics.counter("content.read", "result", "fallback_hit", "operation", operation).increment(); }
    private String externalId(String bookId) { return bookId.startsWith("wenku8-") ? bookId.substring(7) : bookId; }
    private boolean canFallback(String bookId) { return bookId != null && !bookId.startsWith("content-"); }
    private String canonicalBookId(String externalId) { return "wenku8-" + externalId; }
    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private Map<String, Object> canonicalNovel(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>(source); result.put("id", canonicalBookId(string(source.get("id")))); return result;
    }
    @SuppressWarnings("unchecked") private ContentReadService.NovelDto novelDto(Map<String, Object> raw) {
        List<String> tags = raw.get("tags") instanceof List<?> values ? values.stream().map(String::valueOf).toList() : List.of();
        return new ContentReadService.NovelDto(string(raw.get("id")), string(raw.get("title")), string(raw.get("author")),
            string(raw.get("description")), string(raw.get("status")), tags,
            raw.get("copyright") instanceof Boolean value && value, "/content/novels/" + string(raw.get("id")) + "/cover", "UNKNOWN");
    }
}
