package com.sakuya.backend.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Wenku8 首页缓存同步的开关与调度参数；默认每天凌晨按上海时区刷新。 */
@ConfigurationProperties(prefix = "wenku8.catalog.sync")
public record Wenku8CatalogSyncProperties(boolean enabled, boolean startupEnabled, String cron, String zone) { }
