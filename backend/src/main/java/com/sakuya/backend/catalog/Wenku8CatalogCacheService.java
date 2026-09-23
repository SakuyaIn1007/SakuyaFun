package com.sakuya.backend.catalog;

import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.content.ContentHashes;
import com.sakuya.backend.content.ContentStorage;
import com.sakuya.backend.content.cover.Wenku8CoverClient;
import com.sakuya.backend.wenku8.Wenku8GatewayService;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wenku8CatalogCacheService.java
 * 职责说明：将 Wenku8 三个首页栏目同步为本地书目和栏目条目缓存，并把封面字节预取到本地对象存储。
 * 执行流程：任务先读取各栏目列表并去重 -> 直接用列表页资料更新 books（**不再请求任何详情**）->
 * 成功的栏目整体替换排序 -> 返回后在事务外按需并发预取封面。
 *
 * 详情请求已全部移除：适配器两个接口返回的 DTO 键完全相同，唯一的差异是 description
 * （列表页是截断的 intro_preview，详情页是完整的 intro），因此 40–60 次详情请求只买到
 * 「简介更完整」。该字段改为用户真正打开某本书时按需补抓，见 NovelDetailBackfillService。
 *
 * 任一栏目上游失败时不删除该栏目旧缓存，从而保证客户端在 Wenku8 暂不可用时仍有可展示数据。
 */
@Service
public class Wenku8CatalogCacheService {
    private static final Logger log = LoggerFactory.getLogger(Wenku8CatalogCacheService.class);
    /** 封面预取的提交并发上限；真正的在途 HTTP 数由 Wenku8CoverClient 内的信号量约束。 */
    private static final int COVER_SUBMIT_CONCURRENCY = 4;
    /**
     * 封面预取等待上限。封面是**可选增强**，超时后放弃本轮未完成的部分并照常结束同步：
     * 已写入的封面保留，未抓到的下次同步补，绝不能因为封面拖住整个同步任务。
     */
    private static final long COVER_STAGE_TIMEOUT_SECONDS = 120;
    private final Wenku8GatewayService gateway;
    private final BookRepository books;
    private final Wenku8CatalogEntryRepository entries;
    private final Wenku8CoverClient coverClient;
    private final ContentStorage storage;
    /**
     * 本轮同步处理过的书，供事务外的封面阶段复用，避免再查一次库。
     *
     * 注意：该字段使 refreshAll() **不可重入**。当前它只被单线程调度器
     * （Wenku8CatalogSyncRunner.refresh）调用，成立；若将来引入并发触发，
     * 必须改为参数下传或加锁，否则两轮同步会互相覆盖这份映射。
     */
    private volatile Map<String, Book> lastSynced = Map.of();

    public Wenku8CatalogCacheService(Wenku8GatewayService gateway, BookRepository books,
            Wenku8CatalogEntryRepository entries, Wenku8CoverClient coverClient, ContentStorage storage) {
        this.gateway = gateway;
        this.books = books;
        this.entries = entries;
        this.coverClient = coverClient;
        this.storage = storage;
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
        // 三个栏目会包含相同作品，先按 aid 去重，避免对同一本书重复写入。
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
        lastSynced = booksById;
    }

    /**
     * 预取封面字节到本地对象存储，填充 coverObjectKey。
     *
     * **本方法必须由 refreshAll() 的调用方在事务外单独调用，不得从 refreshAll() 内部自调用**：
     * 自调用会绕过 Spring 代理使 @Transactional 语义失效，且数十次网络 I/O 会被包进
     * refreshAll() 的数据库事务，事务时长从毫秒级涨到分钟级，长时间持有连接与 books 行锁。
     */
    public void prefetchCovers() {
        Collection<Book> synced = lastSynced.values();
        if (synced.isEmpty()) return;
        if (!coverClient.enabled()) {
            log.info("Wenku8 封面直连未启用，跳过封面预取（{} 本）", synced.size());
            return;
        }
        log.info("开始预取 Wenku8 封面，共 {} 本", synced.size());
        int stored = 0;
        ExecutorService executor = Executors.newFixedThreadPool(COVER_SUBMIT_CONCURRENCY);
        try {
            List<Future<Boolean>> pending = synced.stream().map(book -> executor.submit(() -> prefetchOne(book))).toList();
            for (Future<Boolean> future : pending) {
                try {
                    if (Boolean.TRUE.equals(future.get(COVER_STAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS))) stored++;
                } catch (Exception error) {
                    // 单本封面失败（含等待超时）不应中断整批；已抓到的封面保留，未抓到的下次同步补。
                    log.debug("Wenku8 封面预取单项失败：{}", error.getMessage());
                }
            }
        } finally {
            executor.shutdownNow();
        }
        log.info("Wenku8 封面预取完成，新增 {} 张，共 {} 本", stored, synced.size());
    }

    /** 单本封面写入；任何失败都只记日志并返回 false，绝不影响书目数据。 */
    private boolean prefetchOne(Book book) {
        // 第一道短路：已有对象键且对象确实存在时完全不做上游请求。这是每日同步的主要收益来源——
        // 没有它，每日同步会退化为每天重新下载全部封面。
        String existing = book.getCoverObjectKey();
        if (existing != null && !existing.isBlank() && storage.exists(existing)) return false;
        String aid = book.getSourceNovelId();
        if (aid == null || !aid.matches("\\d{1,12}")) return false;
        try {
            Wenku8CoverClient.CoverImage image = coverClient.fetch(Integer.parseInt(aid));
            String sha256 = ContentHashes.sha256(image.bytes());
            // 第二道短路：内容未变则只补元数据，不重写存储。
            // 这要求对象键按内容哈希寻址——同哈希同键，此时「跳过写入」才等价于「对象已存在」。
            if (sha256.equals(book.getCoverSha256()) && existing != null && storage.exists(existing)) return false;
            String key = "books/" + book.getId() + "/covers/" + sha256 + extension(image.contentType());
            ContentStorage.StoredObject stored = storage.put(key, image.bytes(), image.contentType());
            updateCover(book.getId(), stored);
            return true;
        } catch (Exception error) {
            log.debug("Wenku8 小说 {} 封面预取失败：{}", aid, error.getMessage());
            return false;
        }
    }

    /**
     * 封面元数据写入。逐本一个独立短事务（save 自带），与 refreshAll() 的事务完全解耦。
     *
     * 必须重新 findById 取当前实体：入参 Book 是 lastSynced 里的快照，
     * 直接用它会覆盖掉本轮同步之外的其他写入。
     */
    private void updateCover(String bookId, ContentStorage.StoredObject stored) {
        books.findById(bookId).ifPresent(current -> {
            current.updateCoverObject(stored.key(), stored.sha256(), stored.byteSize());
            books.save(current);
        });
    }

    /** 按内容类型推导对象键扩展名；ContentReadService 是按对象键后缀判定响应 Content-Type 的。 */
    private String extension(String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(java.util.Locale.ROOT);
        if (type.contains("png")) return ".png";
        if (type.contains("webp")) return ".webp";
        return ".jpg";
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

    /** 只使用列表页资料入库；详情补全由 NovelDetailBackfillService 在用户读取时按需完成。 */
    private Book cache(Map<String, Object> item, Instant syncedAt) {
        String aid = string(item.get("id"));
        if (aid.isBlank()) throw new BusinessException(503, "Wenku8 返回了无效小说标识");
        String id = "wenku8-" + aid;
        List<String> tags = list(item.get("tags"));
        Book book = books.findById(id).orElseGet(() -> new Book(id, string(item.get("title")), string(item.get("author")), "", 0f, tags, string(item.get("description")), null));
        book.refreshFromWenku8(aid, string(item.get("title")), string(item.get("author")),
                tags, string(item.get("description")), string(item.get("status")), bool(item.get("copyright")), null, syncedAt);
        return books.save(book);
    }

    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private boolean bool(Object value) { return value instanceof Boolean flag && flag; }
    private List<String> list(Object value) { return value instanceof List<?> items ? items.stream().map(String::valueOf).toList() : List.of(); }
}
