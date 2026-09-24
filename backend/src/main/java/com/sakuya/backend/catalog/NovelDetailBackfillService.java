package com.sakuya.backend.catalog;

import com.sakuya.backend.wenku8.Wenku8GatewayService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * NovelDetailBackfillService.java
 * 职责说明：在用户真正打开某本书时按需补抓完整简介，取代原先同步阶段对每本书的详情请求。
 * 执行流程：命中的书若从未补全过 -> 原子占位在途标记 -> 提交异步任务 -> 调用适配器详情接口 ->
 * 成功后重新读取实体并窄写入简介与 detailSyncedAt；失败则记录时间戳并在冷却期内不再重排。
 *
 * 为什么必须异步：详情页位于 Cloudflare 验证站且需要登录，只能经适配器访问，
 * 因而受 5 秒/次节流约束、与正文导入共享额度、无法并发。同步补抓会让详情页等待 5 秒以上。
 * 代价是「本次返回预览档、下次才返回完整简介」——这是异步方案的固有代价，已确认接受。
 *
 * 本服务**永不向上抛异常**：补抓是可选增强，任何失败都不得影响详情页的正常返回。
 */
@Service
public class NovelDetailBackfillService {
    private static final Logger log = LoggerFactory.getLogger(NovelDetailBackfillService.class);

    private final Wenku8GatewayService gateway;
    private final BookRepository books;
    /** 在途上限与失败冷却来自配置：两者都与部署规模及上游风控强度相关，不宜写死在业务类里。 */
    private final Wenku8CatalogSyncProperties properties;
    /** 元素为 aid（上游标识），不是 bookId——两个 ID 空间不同，去重必须按上游标识。 */
    private final Set<String> inflight = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> failedAt = new ConcurrentHashMap<>();
    /**
     * 单线程而非多线程：适配器本就是 5 秒/次串行，多线程只是把排队从后端队列搬进适配器的信号量，
     * 却引入 N 个线程同时占用的成本。
     *
     * **拒绝策略必须是 AbortPolicy，不得改成 DiscardPolicy/DiscardOldestPolicy**：
     * DiscardPolicy 静默丢弃且不抛异常，CompletableFuture.runAsync 返回的 future 会**永不完成**，
     * whenComplete 因此永不执行，在该途标记永久泄漏——该书从此再也不会被补抓，且没有任何报错。
     * CallerRunsPolicy 同样不可用：它会在用户的请求线程上同步跑那次 5 秒节流的网络调用。
     */
    private final ExecutorService executor;

    public NovelDetailBackfillService(Wenku8GatewayService gateway, BookRepository books,
            Wenku8CatalogSyncProperties properties) {
        this.gateway = gateway;
        this.books = books;
        this.properties = properties;
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(properties.detailBackfillMaxInflight()), runnable -> {
                Thread thread = new Thread(runnable, "wenku8-detail-backfill");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * 判定并排入一次补抓；命中路径调用，永不抛异常。
     *
     * **读模式判定不在这里**：DB_ONLY 的语义是「不访问上游」，属于读取策略，
     * 由 ContentFallbackFacade 在调用本方法前决定是否调用（那里已经持有 readMode）。
     * 本类只负责「这本书是否需要且允许补抓」。
     */
    public void schedule(String bookId) {
        try {
            Book book = books.findById(bookId).orElse(null);
            // 只处理目录同步入库的 Wenku8 作品。本地书与 content-{UUID} 的来源标记不是 WENKU8，
            // 若无此判定，它们每次被打开都会排一次注定失败的补抓。
            if (book == null || !"WENKU8".equals(book.getSourceType())) return;
            if (book.getDetailSyncedAt() != null) return;
            String aid = book.getSourceNovelId();
            if (aid == null || !aid.matches("\\d{1,12}")) return;
            if (inCooldown(aid)) return;
            if (inflight.size() >= properties.detailBackfillMaxInflight()) {
                log.debug("详情补抓在途已达上限 {}，跳过 {}", properties.detailBackfillMaxInflight(), aid);
                return;
            }
            // 原子占位：add 的返回值即「我是不是第一个」，且此处到提交之间不得有阻塞调用。
            if (!inflight.add(aid)) return;
            submit(aid);
        } catch (Exception error) {
            log.debug("详情补抓调度失败：{}", error.getMessage());
        }
    }

    private void submit(String aid) {
        try {
            // 注销绑定【任务完成】，不绑定调用方的控制流：若写在调用方的 finally 里，
            // 调用方所在请求线程被打断时会提前摘除标记，而任务仍在运行，
            // 第三个请求就会为同一 aid 再排一次，去重直接失效。
            CompletableFuture.runAsync(() -> run(aid), executor)
                .whenComplete((ignored, error) -> inflight.remove(aid));
        } catch (Exception error) {
            // 提交被拒（队列满）时 future 根本未创建，whenComplete 不会执行，必须在这里手动注销。
            inflight.remove(aid);
            log.debug("详情补抓提交被拒，跳过 {}", aid);
        }
    }

    /** 补抓主体，无事务：网络调用与数据库写入都不允许寄生于调用方的读事务。 */
    private void run(String aid) {
        try {
            Map<String, Object> detail = gateway.novel(aid);
            String description = string(detail.get("description"));
            // 上游返回空简介时仍然写入标记：否则每次打开都会重抓一本没有简介的书，
            // 持续消耗与正文导入共享的 5 秒节流额度。
            Instant now = Instant.now();
            // 重新 findById 取当前实体，而不是复用 schedule 阶段的快照——实体不该跨网络调用持有。
            books.findById("wenku8-" + aid).ifPresent(book -> {
                book.applyDetailDescription(description, now);
                books.save(book);
            });
            failedAt.remove(aid);
            log.info("Wenku8 小说 {} 详情补抓完成", aid);
        } catch (Exception error) {
            failedAt.put(aid, Instant.now());
            log.info("Wenku8 小说 {} 详情补抓失败，{} 秒后可重试：{}",
                aid, properties.detailBackfillCooldownSeconds(), error.getMessage());
        }
    }

    /** 冷却判定顺带清理过期条目，避免 map 无界增长；每本书至多一条，规模与书目数同阶。 */
    private boolean inCooldown(String aid) {
        Instant failed = failedAt.get(aid);
        if (failed == null) return false;
        if (Instant.now().isBefore(failed.plus(Duration.ofSeconds(properties.detailBackfillCooldownSeconds())))) return true;
        failedAt.remove(aid);
        return false;
    }

    /** 仅测试用：验证在途去重与泄漏。 */
    public int inflightCount() { return inflight.size(); }

    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
}
