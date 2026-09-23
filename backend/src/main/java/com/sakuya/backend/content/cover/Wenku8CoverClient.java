package com.sakuya.backend.content.cover;

import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.content.CloudflareChallenge;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.function.UnaryOperator;

/**
 * Wenku8CoverClient.java
 * 职责说明：按 aid 直连 img.wenku8.com 取封面字节；是封面 URL 形状的唯一持有处。
 * 执行流程：校验开关与 aid -> 取得并发许可 -> 构造 URL 发起 GET -> 体积/质询/状态码判定 -> 返回字节与内容类型。
 *
 * 与 DownloadSourceClient 的关系：两者同为「后端直连 CDN」，但**刻意不合并**（独立开关、独立类），
 * 且语义有一处关键差异——本文档**不做节点切换**。正文 CDN 有 dl1/dl2 两个可用节点，
 * 封面域 img.wenku8.com **只有一个**，没有等价的重试目标；单次失败即失败，交由上层记日志并跳过。
 * 这不是遗漏：为封面引入 429 重试只会延长同步耗时，而封面的失败代价仅是「首屏这一张多一次回源」。
 */
public class Wenku8CoverClient {

    /** 浏览器 UA：CDN 二进制资源正常仅校验 UA，不校验登录态（对照 pywenku8api 的 _fetch_binary 文档）。 */
    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private final HttpClient httpClient;
    private final Wenku8CoverProperties properties;
    /** 测试可注入的 URL 改写函数；生产环境为恒等映射。 */
    private final UnaryOperator<String> urlRewriter;
    /** 并发闸放在客户端内部：这样能约束所有调用方，放在编排层只能约束编排方。 */
    private final Semaphore concurrency;

    public Wenku8CoverClient(HttpClient httpClient, Wenku8CoverProperties properties) {
        this(httpClient, properties, UnaryOperator.identity());
    }

    Wenku8CoverClient(HttpClient httpClient, Wenku8CoverProperties properties, UnaryOperator<String> urlRewriter) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.urlRewriter = urlRewriter;
        this.concurrency = new Semaphore(properties.maxConcurrency());
    }

    /**
     * 封面路径按 aid 千位分目录：4386 -> /4/4386/4386s.jpg，35 -> /0/35/35s.jpg。
     * 该形状已由 4 个真实样本验证（4386/35/3470/2751），与 pywenku8api 的 get_novel_cover 拼法一致。
     * 公式失效时表现为「封面缺失但书目正常入库」，是可观测的降级而非静默损坏。
     *
     * 注意协议：上游列表页中的地址是 http://，此处按 https:// 走，不照抄页面。
     */
    public static String coverUrl(int aid) {
        return "https://img.wenku8.com/image/" + (aid / 1000) + "/" + aid + "/" + aid + "s.jpg";
    }

    public boolean enabled() { return properties.enabled(); }

    public CoverImage fetch(int aid) {
        if (!properties.enabled()) throw new BusinessException(503, "Wenku8 封面直连未启用");
        if (aid <= 0) throw new BusinessException(400, "小说标识不合法");
        try {
            concurrency.acquire();
        } catch (InterruptedException error) {
            // 与 DownloadSourceClient 同口径：恢复中断位，否则上层的取消信号会被静默吞掉。
            Thread.currentThread().interrupt();
            throw new BusinessException(503, "Wenku8 封面读取被中断");
        }
        try {
            return fetch(coverUrl(aid));
        } finally {
            concurrency.release();
        }
    }

    private CoverImage fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(urlRewriter.apply(url)))
            .timeout(Duration.ofSeconds(properties.readTimeoutSeconds()))
            .header("User-Agent", USER_AGENT)
            .GET().build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            byte[] body = response.body();
            if (body != null && body.length > properties.maxBytes()) {
                throw new BusinessException(503, "Wenku8 封面超出体积上限");
            }
            // 质询判定必须先于状态码分支：CF 拦截以 403/503 返回 HTML，若先按状态码分类会被误报为上游异常，
            // 更严重的是会把这页 HTML 当成图片字节写进对象存储。
            if (CloudflareChallenge.isChallenge(status, body)) {
                throw new BusinessException(503, "封面资源被 Cloudflare 防火墙拦截");
            }
            if (status == 429) throw new BusinessException(429, "Wenku8 封面源限流");
            if (status == 404) throw new BusinessException(404, "该小说没有封面");
            if (status != 200) throw new BusinessException(503, "Wenku8 封面源返回异常状态：" + status);
            if (body == null || body.length == 0) throw new BusinessException(503, "Wenku8 封面内容为空");
            return new CoverImage(body, contentType(response));
        } catch (BusinessException error) {
            throw error;
        } catch (java.net.http.HttpTimeoutException error) {
            throw new BusinessException(504, "Wenku8 封面源响应超时");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new BusinessException(503, "Wenku8 封面读取被中断");
        } catch (Exception error) {
            throw new BusinessException(503, "Wenku8 封面源不可用：" + error.getMessage());
        }
    }

    /**
     * 内容类型决定对象键的扩展名，而 ContentReadService.coverContentType() 是**按对象键后缀**判定响应头的：
     * 若一律写 .jpg 而实际是 PNG，响应头会与实际字节不符。
     * 因此这里必须读取上游 Content-Type，缺失或非 image/* 时回退 image/jpeg。
     */
    private String contentType(HttpResponse<?> response) {
        String raw = response.headers().firstValue("Content-Type").orElse("");
        // 形如 "image/png; charset=binary"，只取分号前的类型部分并小写化。
        String type = raw.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return type.startsWith("image/") ? type : "image/jpeg";
    }

    public record CoverImage(byte[] bytes, String contentType) { }
}
