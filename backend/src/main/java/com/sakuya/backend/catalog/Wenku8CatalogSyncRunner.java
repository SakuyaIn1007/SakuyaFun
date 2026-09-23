package com.sakuya.backend.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** 将可能耗时的上游同步放入 Spring 管理的异步线程，避免阻塞应用就绪和定时调度线程。 */
@Service
public class Wenku8CatalogSyncRunner {
    private static final Logger log = LoggerFactory.getLogger(Wenku8CatalogSyncRunner.class);
    private final Wenku8CatalogCacheService cache;
    public Wenku8CatalogSyncRunner(Wenku8CatalogCacheService cache) { this.cache = cache; }
    @Async
    public void refresh(String trigger) {
        log.info("开始 {} Wenku8 首页缓存同步", trigger);
        cache.refreshAll();
        log.info("Wenku8 首页缓存同步完成");
        // 封面预取必须在 refreshAll() 返回之后单独调用：refreshAll() 带 @Transactional，
        // 内部自调用会绕过 Spring 代理，且数十次网络 I/O 会被包进同一个数据库事务。
        cache.prefetchCovers();
    }
}
