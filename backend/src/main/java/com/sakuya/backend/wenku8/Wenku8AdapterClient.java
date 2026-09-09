package com.sakuya.backend.wenku8;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakuya.backend.common.BusinessException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Wenku8AdapterClient.java
 * 职责说明：负责 Spring Boot 到 Python 内网适配服务的 HTTP 调用和错误翻译。
 * 执行流程：GatewayService 传入已校验参数 -> 本类构造超时请求 -> JSON 转 Map -> 非成功响应转换为业务异常。
 */
@Component
public class Wenku8AdapterClient {
    private final Wenku8Properties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public Wenku8AdapterClient(Wenku8Properties properties, ObjectMapper objectMapper) { this.properties = properties; this.objectMapper = objectMapper; }
    public Map<String, Object> get(String path) { return get(path, Duration.ofSeconds(90)); }
    /** 全文接口单独使用可配置的长超时，普通搜索/目录仍维持 90 秒上限。 */
    public Map<String, Object> getFullContent(String path) { return get(path, Duration.ofSeconds(properties.fullContentTimeoutSeconds())); }
    /** 封面等二进制资产只供后台导入，客户端不会接触适配器地址。 */
    public byte[] getBytes(String path) {
        if (!properties.enabled() || properties.normalizedBaseUrl().isBlank()) throw new BusinessException(503, "Wenku8 内部验证服务未启用");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.normalizedBaseUrl() + path))
                .timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) return response.body();
            throw new BusinessException(response.statusCode() == 404 ? 404 : 503, "Wenku8 封面暂不可用");
        } catch (BusinessException error) { throw error;
        } catch (Exception error) { throw new BusinessException(503, "无法读取 Wenku8 封面"); }
    }

    /**
     * 使用 InputStream 解析内部 JSON，避免整本正文在 HTTP Body 字符串和 Jackson 之间产生两份大对象副本。
     * 调用方仍会得到稳定 DTO，正文不会被持久化到数据库或文件系统。
     */
    public Map<String, Object> get(String path, Duration responseTimeout) {
        if (!properties.enabled() || properties.normalizedBaseUrl().isBlank()) throw new BusinessException(503, "Wenku8 内部验证服务未启用");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.normalizedBaseUrl() + path))
                .timeout(responseTimeout).header("Accept", "application/json").GET().build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return objectMapper.readValue(body, new TypeReference<>() {});
                }
                String message = "Wenku8 服务暂不可用";
                try {
                    Map<String, Object> error = objectMapper.readValue(body, new TypeReference<>() {});
                    Object detail = error.get("detail");
                    if (detail instanceof Map<?, ?> map && map.get("message") != null) message = String.valueOf(map.get("message"));
                } catch (Exception ignored) { }
                int status = response.statusCode() == 404 ? 404 : response.statusCode() == 504 ? 504 : 503;
                throw new BusinessException(status, message);
            }
        } catch (BusinessException error) { throw error;
        } catch (java.net.http.HttpTimeoutException error) { throw new BusinessException(504, "Wenku8 服务响应超时");
        } catch (Exception error) { throw new BusinessException(503, "无法连接 Wenku8 内部验证服务"); }
    }
    public static String query(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
