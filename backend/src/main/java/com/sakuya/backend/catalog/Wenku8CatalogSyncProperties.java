package com.sakuya.backend.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Wenku8CatalogSyncProperties.java
 * 职责说明：目录同步的开关与调度参数，以及按需详情补抓的资源参数；默认每天凌晨按上海时区刷新。
 * 执行流程：Spring 读取环境配置 -> 调度器判断是否执行同步 -> 补抓服务读取在途上限与失败冷却。
 *
 * 补抓参数与同步参数放在同一前缀下，是因为两者同属 catalog 子系统的上游访问预算，
 * 且都与适配器 5 秒/次的节流额度直接相关——分开配置会让「谁在抢额度」变得难以判断。
 */
@ConfigurationProperties(prefix = "wenku8.catalog.sync")
public record Wenku8CatalogSyncProperties(boolean enabled, boolean startupEnabled, String cron, String zone,
        int detailBackfillMaxInflight, int detailBackfillCooldownSeconds) {

    public Wenku8CatalogSyncProperties {
        // 在途上限决定同时排队的补抓任务数；超过则直接放弃排队而不堆积。
        if (detailBackfillMaxInflight <= 0) detailBackfillMaxInflight = 64;
        // 失败冷却：上游被持续拦截时，避免用户来回切页把 5 秒/次的节流额度吃光。
        if (detailBackfillCooldownSeconds <= 0) detailBackfillCooldownSeconds = 60;
    }
}
