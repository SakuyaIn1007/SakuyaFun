package com.sakuya.backend.content.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sakuya.backend.common.BusinessException;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * DownloadSourceClientTest.java
 * 职责说明：用本地 HTTP 桩验证节点回退、Cloudflare 质询检测与错误分类，不访问真实 CDN。
 * 执行流程：启动本地 HttpServer 返回预设响应 -> 调用客户端 -> 断言请求次数与异常语义。
 */
class DownloadSourceClientTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            if (path.contains("/429/")) {
                exchange.sendResponseHeaders(429, -1);
            } else if (path.contains("/cf/")) {
                byte[] body = "<html><head><title>Just a moment...</title></head><body></body></html>"
                    .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(403, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
            } else if (path.contains("/ok/")) {
                byte[] body = "正文内容".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private DownloadSourceClient clientAt(String pathPrefix) {
        DownloadSourceProperties properties = new DownloadSourceProperties(true, 5, 30, 64 * 1024 * 1024);
        // 以 URL 改写把请求指向本地桩，避免测试触碰真实 CDN。
        UnaryOperator<String> rewrite = url -> baseUrl + pathPrefix + "/" + url;
        return new DownloadSourceClient(HttpClient.newHttpClient(), properties, rewrite);
    }

    @Test
    void 正常下载返回字节() {
        DownloadSourceClient client = clientAt("/ok");
        assertThat(new String(client.download(1234), StandardCharsets.UTF_8)).isEqualTo("正文内容");
    }

    @Test
    void 两个节点均429时抛出明确错误() {
        DownloadSourceClient client = clientAt("/429");
        assertThatThrownBy(() -> client.download(1234))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("限流");
        // 节点 1 与节点 2 各请求一次
        assertThat(requests.get()).isEqualTo(2);
    }

    @Test
    void 遇到Cloudflare质询页抛出明确错误且不重试节点() {
        DownloadSourceClient client = clientAt("/cf");
        assertThatThrownBy(() -> client.download(1234))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Cloudflare");
        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void 未找到资源抛出404语义错误() {
        DownloadSourceClient client = clientAt("/missing");
        assertThatThrownBy(() -> client.download(1234))
            .isInstanceOf(BusinessException.class)
            .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(404));
    }

    @Test
    void 关闭开关时拒绝下载() {
        DownloadSourceProperties disabled = new DownloadSourceProperties(false, 5, 30, 64 * 1024 * 1024);
        DownloadSourceClient client = new DownloadSourceClient(
            HttpClient.newHttpClient(), disabled, url -> baseUrl + "/ok/" + url);
        assertThatThrownBy(() -> client.download(1234))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("未启用");
    }

    @Test
    void URL形状符合CDN约定() {
        assertThat(DownloadSourceClient.fileUrl(1, 1234)).isEqualTo("https://dl1.wenku8.com/txtutf8/1/1234.txt");
        assertThat(DownloadSourceClient.fileUrl(2, 3020)).isEqualTo("https://dl2.wenku8.com/txtutf8/3/3020.txt");
    }
}
