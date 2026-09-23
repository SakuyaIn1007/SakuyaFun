package com.sakuya.backend.content.cover;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Wenku8CoverProperties.java
 * 职责说明：集中维护封面直连通道的开关、超时、体积上限与并发上限。
 * 执行流程：Spring 读取环境配置 -> Client 在取图前检查 enabled -> 按 maxConcurrency 限制同时在途请求。
 *
 * 与 DownloadSourceProperties 平行但**互不合并**：封面预取是可选增强（失败只是首屏多一次回源），
 * 全文回填是读取链路的兜底（失败则该书不可读），合并开关会迫使「只想关封面」连带关掉正文兜底。
 */
@ConfigurationProperties(prefix = "wenku8.cover-source")
public record Wenku8CoverProperties(boolean enabled, int connectTimeoutSeconds, int readTimeoutSeconds,
        int maxBytes, int maxConcurrency) {

    public Wenku8CoverProperties {
        if (connectTimeoutSeconds <= 0) connectTimeoutSeconds = 5;
        // 封面是 90×130 级别的缩略图，10 秒足够；过长会在上游抖动时拖住整个同步批次。
        if (readTimeoutSeconds <= 0) readTimeoutSeconds = 10;
        // 实测封面约 20–60 KB；2 MiB 留出约 30 倍余量，同时防止异常响应耗尽内存。
        if (maxBytes <= 0) maxBytes = 2 * 1024 * 1024;
        // 并发是「避免打爆上游」的上限，不是吞吐目标；封面域无需正文那样的节流，小并发即可。
        if (maxConcurrency <= 0) maxConcurrency = 4;
    }
}
