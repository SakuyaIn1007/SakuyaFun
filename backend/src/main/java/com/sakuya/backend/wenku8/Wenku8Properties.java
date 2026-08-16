package com.sakuya.backend.wenku8;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Wenku8Properties.java
 * 职责说明：集中维护 Wenku8 内部适配服务地址和启用状态，避免将实验性上游地址散落在业务代码中。
 * 执行流程：Spring 读取环境配置 -> GatewayService 在每次请求前检查 enabled -> AdapterClient 按接口类型发起内网调用。
 */
@ConfigurationProperties(prefix = "wenku8.adapter")
public record Wenku8Properties(String baseUrl, boolean enabled, int fullContentTimeoutSeconds) {
    /** 全文包含整本正文，等待时间必须覆盖 Python 的全文下载与目录读取，但限制最大等待避免请求永久占用。 */
    public Wenku8Properties {
        if (fullContentTimeoutSeconds < 75) fullContentTimeoutSeconds = 195;
    }
    public String normalizedBaseUrl() { return baseUrl == null ? "" : baseUrl.replaceAll("/+$", ""); }
}
