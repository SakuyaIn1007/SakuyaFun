package com.sakuya.backend.content;

import com.sakuya.backend.common.BusinessException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** 根据部署配置创建唯一 ContentStorage，业务代码不感知本地文件或 S3。 */
@Configuration
public class ContentStorageConfiguration {
    @Bean
    ContentStorage contentStorage(ContentStorageProperties properties) {
        return switch (properties.normalizedType()) {
            case "local" -> new LocalContentStorage(properties.normalizedLocalDir());
            case "s3", "minio" -> new S3ContentStorage(properties);
            default -> throw new IllegalStateException("不支持的内容存储类型：" + properties.type());
        };
    }
}

/** 本地实现严格限制对象键不能逃逸根目录，适合开发与单机部署。 */
final class LocalContentStorage implements ContentStorage {
    private final Path root;
    LocalContentStorage(Path root) {
        if (root == null) throw new IllegalArgumentException("内容存储根目录不能为空");
        // 构造时固定绝对根目录，确保直接实例化和配置注入两条路径都使用相同的逃逸判断语义。
        this.root = root.toAbsolutePath().normalize();
    }
    @Override public StoredObject put(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            return new StoredObject(key, content.length, ContentHashes.sha256(content));
        } catch (IOException error) { throw new BusinessException(503, "内容文件写入失败"); }
    }
    @Override public byte[] read(String key) {
        try { return Files.readAllBytes(resolve(key)); }
        catch (IOException error) { throw new BusinessException(404, "内容文件不存在"); }
    }
    @Override public boolean exists(String key) { return Files.isRegularFile(resolve(key)); }
    private Path resolve(String key) {
        if (key == null || key.isBlank()) throw new BusinessException(404, "内容文件不存在");
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) throw new BusinessException(400, "内容对象键不合法");
        return target;
    }
}

/** S3 兼容实现使用 path-style，可同时服务 AWS S3 与 MinIO。 */
final class S3ContentStorage implements ContentStorage {
    private final S3Client client;
    private final String bucket;
    private volatile boolean bucketReady;
    S3ContentStorage(ContentStorageProperties properties) {
        var builder = S3Client.builder()
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .region(Region.of(properties.normalizedRegion()))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        if (properties.endpoint() != null && !properties.endpoint().isBlank()) builder.endpointOverride(URI.create(properties.endpoint()));
        if (properties.accessKey() != null && !properties.accessKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())));
        }
        client = builder.build(); bucket = properties.normalizedBucket();
    }
    @Override public StoredObject put(String key, byte[] content, String contentType) {
        ensureBucket();
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(), RequestBody.fromBytes(content));
        return new StoredObject(key, content.length, ContentHashes.sha256(content));
    }
    @Override public byte[] read(String key) {
        try { return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray(); }
        catch (NoSuchKeyException error) { throw new BusinessException(404, "内容文件不存在"); }
        catch (S3Exception error) { throw new BusinessException(error.statusCode() == 404 ? 404 : 503, "内容存储读取失败"); }
    }
    @Override public boolean exists(String key) {
        try { client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()); return true; }
        catch (S3Exception error) { if (error.statusCode() == 404) return false; throw new BusinessException(503, "内容存储检查失败"); }
    }
    private synchronized void ensureBucket() {
        if (bucketReady) return;
        try { client.headBucket(HeadBucketRequest.builder().bucket(bucket).build()); }
        catch (S3Exception error) {
            if (error.statusCode() != 404) throw new BusinessException(503, "内容存储不可用");
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
        bucketReady = true;
    }
}

final class ContentHashes {
    private ContentHashes() { }
    static String sha256(byte[] content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
        catch (Exception error) { throw new IllegalStateException("SHA-256 不可用", error); }
    }
}
