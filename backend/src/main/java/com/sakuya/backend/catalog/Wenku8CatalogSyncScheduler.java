package com.sakuya.backend.catalog;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Wenku8CatalogSyncScheduler.java
 * 职责说明：在后端就绪后和每日定时触发 Wenku8 本地缓存同步。
 * 执行流程：应用就绪事件立即异步同步一次 -> 每日 cron 再次异步执行 -> 同步服务保留所有失败栏目的旧数据。
 */
@Component
public class Wenku8CatalogSyncScheduler {
    private final Wenku8CatalogSyncRunner runner;
    private final Wenku8CatalogSyncProperties properties;

    public Wenku8CatalogSyncScheduler(Wenku8CatalogSyncRunner runner, Wenku8CatalogSyncProperties properties) {
        this.runner = runner;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (properties.enabled() && properties.startupEnabled()) runner.refresh("启动");
    }

    @Scheduled(cron = "${wenku8.catalog.sync.cron:0 30 3 * * *}", zone = "${wenku8.catalog.sync.zone:Asia/Shanghai}")
    public void syncDaily() {
        if (properties.enabled()) runner.refresh("定时");
    }
}
