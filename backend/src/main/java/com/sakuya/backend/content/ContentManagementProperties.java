package com.sakuya.backend.content;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 内容管理令牌与读取切换模式；DB_ONLY 可在完成迁移后彻底关闭在线适配器回退。 */
@ConfigurationProperties(prefix = "app.content-management")
public record ContentManagementProperties(String token, String readMode) {
    public String normalizedReadMode() {
        return readMode == null || readMode.isBlank() ? "DB_FIRST_WITH_ADAPTER_FALLBACK" : readMode.trim().toUpperCase();
    }
}
