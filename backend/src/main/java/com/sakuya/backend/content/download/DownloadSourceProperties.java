package com.sakuya.backend.content.download;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DownloadSourceProperties.java
 * 职责说明：集中维护 CDN 下载源的开关、超时与体积上限，避免把上游地址写死在业务代码中。
 * 执行流程：Spring 读取环境配置 -> Client 在每次下载前检查 enabled -> 按节点顺序发起请求。
 */
@ConfigurationProperties(prefix = "wenku8.download-source")
public record DownloadSourceProperties(boolean enabled, int connectTimeoutSeconds, int readTimeoutSeconds, int maxBytes) {
    public DownloadSourceProperties {
        if (connectTimeoutSeconds <= 0) connectTimeoutSeconds = 5;
        if (readTimeoutSeconds <= 0) readTimeoutSeconds = 30;
        // 整本 TXT 可达数 MB；上限用于防止异常响应耗尽内存。
        if (maxBytes <= 0) maxBytes = 64 * 1024 * 1024;
    }
}
