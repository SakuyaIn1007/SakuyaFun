package com.sakuya.backend.content;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 内容对象存储配置；local 用于单机开发，s3 可连接 AWS S3 或启用 path-style 的 MinIO。 */
@ConfigurationProperties(prefix = "app.content-storage")
public record ContentStorageProperties(
    String type, String localDir, String endpoint, String region, String bucket, String accessKey, String secretKey
) {
    public String normalizedType() { return type == null || type.isBlank() ? "local" : type.trim().toLowerCase(); }
    public Path normalizedLocalDir() { return Path.of(localDir == null || localDir.isBlank() ? "./content-store" : localDir).toAbsolutePath().normalize(); }
    public String normalizedRegion() { return region == null || region.isBlank() ? "us-east-1" : region; }
    public String normalizedBucket() { return bucket == null || bucket.isBlank() ? "sakuya-content" : bucket; }
}
