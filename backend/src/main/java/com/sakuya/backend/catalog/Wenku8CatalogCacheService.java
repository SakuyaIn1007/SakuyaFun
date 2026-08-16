package com.sakuya.backend.catalog;

import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.wenku8.Wenku8GatewayService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wenku8CatalogCacheService.java
 * 职责说明：将 Wenku8 三个首页栏目同步为本地书目和栏目条目缓存。
 * 执行流程：任务先读取各栏目列表并去重 -> 逐本读取详情补全资料 -> 更新 books -> 成功的栏目整体替换排序；
 * 任一栏目上游失败时不删除该栏目旧缓存，从而保证客户端在 Wenku8 暂不可用时仍有可展示数据。
 */
@Service
public class Wenku8CatalogCacheService {
    private static final Logger log = LoggerFactory.getLogger(Wenku8CatalogCacheService.class);
    private final Wenku8GatewayService gateway;
    private final BookRepository books;
    private final Wenku8CatalogEntryRepository entries;

    public Wenku8CatalogCacheService(Wenku8GatewayService gateway, BookRepository books, Wenku8CatalogEntryRepository entries) {
        this.gateway = gateway;
        this.books = books;
        this.entries = entries;
    }

    /** 同步全部栏目；一个栏目的失败不会影响另外两个栏目的新缓存。 */
    @Transactional
    public void refreshAll() {
        Map<Wenku8CatalogFeed, List<Map<String, Object>>> feedItems = new LinkedHashMap<>();
        for (Wenku8CatalogFeed feed : Wenku8CatalogFeed.values()) {
            try {
                feedItems.put(feed, gateway.list(feed.sort()).items());
            } catch (Exception error) {
                log.warn("Wenku8 {} 栏目同步失败，保留旧缓存：{}", feed.name(), error.getMessage());
            }
        }
        Instant syncedAt = Instant.now();
        Map<String, Book> booksById = new LinkedHashMap<>();
        // 三个栏目会包含相同作品，先按 aid 去重后再请求详情，避免对上游发出重复请求。
        for (List<Map<String, Object>> list : feedItems.values()) {
            for (Map<String, Object> item : list) {
                String aid = string(item.get("id"));
                if (aid.isBlank()) throw new BusinessException(503, "Wenku8 返回了无效小说标识");
                booksById.computeIfAbsent(aid, ignored -> cache(item, syncedAt));
            }
        }
        for (Map.Entry<Wenku8CatalogFeed, List<Map<String, Object>>> feed : feedItems.entrySet()) {
            replaceEntries(feed.getKey(), feed.getValue(), booksById, syncedAt);
        }
    }

    /** 仅替换本次成功取得列表的栏目，网络失败的栏目不会进入此方法。 */
    private void replaceEntries(Wenku8CatalogFeed feed, List<Map<String, Object>> list, Map<String, Book> booksByAid, Instant syncedAt) {
        entries.deleteByFeed(feed.name());
        int position = 0;
        for (Map<String, Object> item : list) {
            Book book = booksByAid.get(string(item.get("id")));
            if (book != null) entries.save(new Wenku8CatalogEntry(feed, book.getId(), position++, syncedAt));
        }
        log.info("Wenku8 {} 栏目同步完成，共 {} 本", feed.name(), position);
    }

    /** 首页接口只读本地关联表；entry 可能引用被人工删除的书，遇到时安全跳过。 */
    public List<Book> cached(Wenku8CatalogFeed feed) {
        return entries.findByFeedOrderByDisplayOrderAsc(feed.name()).stream()
                .map(entry -> books.findById(entry.getBookId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private Book cache(Map<String, Object> item, Instant syncedAt) {
        String aid = string(item.get("id"));
        if (aid.isBlank()) throw new BusinessException(503, "Wenku8 返回了无效小说标识");
        String id = "wenku8-" + aid;
        Map<String, Object> detail = detailOrNull(aid);
        Map<String, Object> data = detail == null ? item : detail;
        List<String> tags = list(data.get("tags"));
        Book book = books.findById(id).orElseGet(() -> new Book(id, string(item.get("title")), string(item.get("author")), "", 0f, list(item.get("tags")), string(item.get("description")), null));
        book.refreshFromWenku8(aid, nonBlank(data.get("title"), item.get("title")), nonBlank(data.get("author"), item.get("author")),
                tags, string(item.get("description")), string(data.get("status")), bool(data.get("copyright")),
                detail == null ? null : string(detail.get("description")), syncedAt);
        return books.save(book);
    }

    /** 详情失败不应中断列表入库；已有完整简介会由 Book.refreshFromWenku8 保留。 */
    private Map<String, Object> detailOrNull(String aid) {
        try { return gateway.novel(aid); }
        catch (Exception error) { log.info("Wenku8 小说 {} 详情补全失败，本次保留已有资料：{}", aid, error.getMessage()); return null; }
    }
    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private String nonBlank(Object preferred, Object fallback) { String value = string(preferred); return value.isBlank() ? string(fallback) : value; }
    private boolean bool(Object value) { return value instanceof Boolean flag && flag; }
    private List<String> list(Object value) { return value instanceof List<?> items ? items.stream().map(String::valueOf).toList() : List.of(); }
}
