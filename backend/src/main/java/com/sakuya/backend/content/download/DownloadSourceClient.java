package com.sakuya.backend.content.download;

import com.sakuya.backend.common.BusinessException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.UnaryOperator;

/**
 * DownloadSourceClient.java
 * 职责说明：按 aid 从 CDN 取整本 UTF-8 TXT，不携带任何凭据，不经过需要 Cloudflare 验证的站点域名。
 * 执行流程：校验开关与 aid -> 依次尝试节点 1、2 -> 仅在 429 时切换节点 -> 返回原始字节。
 *
 * 与 pywenku8api 的既有语义保持一致：只有 429 才做节点回退，其余错误直接抛出，
 * 避免把版权受限（4xx）误当成限流而反复重试。
 */
public class DownloadSourceClient {

    private final HttpClient httpClient;
    private final DownloadSourceProperties properties;
    /** 测试可注入的 URL 改写函数；生产环境为恒等映射。 */
    private final UnaryOperator<String> urlRewriter;

    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    public DownloadSourceClient(HttpClient httpClient, DownloadSourceProperties properties) {
        this(httpClient, properties, UnaryOperator.identity());
    }

    DownloadSourceClient(HttpClient httpClient, DownloadSourceProperties properties, UnaryOperator<String> urlRewriter) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.urlRewriter = urlRewriter;
    }

    /** CDN 路径按 aid 千位分目录：1234 -> /1/1234.txt，3020 -> /3/3020.txt。 */
    public static String fileUrl(int node, int aid) {
        return "https://dl" + node + ".wenku8.com/txtutf8/" + (aid / 1000) + "/" + aid + ".txt";
    }

    public byte[] download(int aid) {
        if (!properties.enabled()) throw new BusinessException(503, "Wenku8 下载源未启用");
        if (aid <= 0) throw new BusinessException(400, "小说标识不合法");
        BusinessException lastRateLimit = null;
        for (int node : new int[] {1, 2}) {
            try {
                return fetch(fileUrl(node, aid));
            } catch (BusinessException error) {
                if (error.getCode() != 429) throw error;
                lastRateLimit = error;
            }
        }
        throw lastRateLimit == null ? new BusinessException(503, "Wenku8 下载源不可用") : lastRateLimit;
    }

    private byte[] fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(urlRewriter.apply(url)))
            .timeout(Duration.ofSeconds(properties.readTimeoutSeconds()))
            .header("User-Agent", USER_AGENT)
            .GET().build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            byte[] body = response.body();
            if (body != null && body.length > properties.maxBytes()) {
                throw new BusinessException(503, "Wenku8 下载内容超出体积上限");
            }
            // 质询判定必须先于状态码分支：CF 拦截以 403/503 返回 HTML，若先按状态码分类会被误报为上游异常。
            if (isCloudflareChallenge(status, body)) {
                throw new BusinessException(503, "CDN 资源被 Cloudflare 防火墙拦截");
            }
            if (status == 429) throw new BusinessException(429, "Wenku8 下载源限流");
            if (status == 404) throw new BusinessException(404, "该小说没有可下载的 TXT");
            if (status != 200) throw new BusinessException(503, "Wenku8 下载源返回异常状态：" + status);
            return body == null ? new byte[0] : body;
        } catch (BusinessException error) {
            throw error;
        } catch (java.net.http.HttpTimeoutException error) {
            throw new BusinessException(504, "Wenku8 下载源响应超时");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new BusinessException(503, "Wenku8 下载被中断");
        } catch (Exception error) {
            throw new BusinessException(503, "Wenku8 下载源不可用：" + error.getMessage());
        }
    }

    /** CF 质询/封禁页会以 403/503 返回 HTML，检测特征串即可与正常响应区分。 */
    private boolean isCloudflareChallenge(int status, byte[] body) {
        if (status != 403 && status != 503) return false;
        if (body == null || body.length == 0) return false;
        String head = new String(body, 0, Math.min(body.length, 4096), StandardCharsets.UTF_8).toLowerCase();
        return head.contains("just a moment") || head.contains("cf-error-details")
            || head.contains("access denied") || head.contains("used cloudflare to restrict access");
    }
}
