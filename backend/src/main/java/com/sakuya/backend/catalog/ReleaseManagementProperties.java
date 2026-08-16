package com.sakuya.backend.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 部署端维护令牌。未配置时所有写接口拒绝请求，避免在无角色模型阶段误开放后台写权限。 */
@ConfigurationProperties(prefix = "app.release-management")
public record ReleaseManagementProperties(String token) { }
