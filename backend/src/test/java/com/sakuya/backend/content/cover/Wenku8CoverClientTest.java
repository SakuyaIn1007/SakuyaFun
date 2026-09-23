package com.sakuya.backend.content.cover;

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
 * Wenku8CoverClientTest.java
 * 职责说明：用本地 HTTP 桩验证封面 URL 形状、内容类型推导、体积上限与错误分类，不访问真实 CDN。
 * 执行流程：启动本地 HttpServer 返回预设响应或 Content-Type -> 调用客户端 -> 断言异常语义与请求次数。
 */
class Wenku8CoverClientTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            if (path.contains("/cf/")) {
                byte[] body = "<html><title>Just a moment...</title></html>".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(403, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
            } else if (path.contains("/png/")) {
                byte[] body = {(byte) 0x89, 'P', 'N', 'G'};
                exchange.getResponseHeaders().add("Content-Type", "image/png");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
            } else if (path.contains("/noctype/")) {
                byte[] body = {(byte) 0xFF, (byte) 0xD8};
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
            } else if (path.contains("/htmltype/")) {
                byte[] body = "not an image".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
            } else if (path.contains("/huge/")) {
                byte[] body = new byte[64];
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
            } else if (path.contains("/empty/")) {
                exchange.sendResponseHeaders(200, -1);
            } else if (path.contains("/429/")) {
                exchange.sendResponseHeaders(429, -1);
            } else if (path.contains("/ok/")) {
                byte[] body = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
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
    void stopServer() { server.stop(0); }

    private Wenku8CoverClient clientAt(String pathPrefix, int maxBytes) {
        Wenku8CoverProperties properties = new Wenku8CoverProperties(true, 5, 10, maxBytes, 4);
        // 以 URL 改写把请求指向本地桩，避免测试触碰真实 CDN。
        UnaryOperator<String> rewrite = url -> baseUrl + pathPrefix + "/" + url;
        return new Wenku8CoverClient(HttpClient.newHttpClient(), properties, rewrite);
    }

    private Wenku8CoverClient clientAt(String pathPrefix) { return clientAt(pathPrefix, 2 * 1024 * 1024); }

    @Test
    void 封面URL按aid千位分目录() {
        assertThat(Wenku8CoverClient.coverUrl(4386)).isEqualTo("https://img.wenku8.com/image/4/4386/4386s.jpg");
        assertThat(Wenku8CoverClient.coverUrl(35)).isEqualTo("https://img.wenku8.com/image/0/35/35s.jpg");
        assertThat(Wenku8CoverClient.coverUrl(3470)).isEqualTo("https://img.wenku8.com/image/3/3470/3470s.jpg");
        assertThat(Wenku8CoverClient.coverUrl(2751)).isEqualTo("https://img.wenku8.com/image/2/2751/2751s.jpg");
    }

    @Test
    void 正常取回字节与内容类型() {
        Wenku8CoverClient.CoverImage image = clientAt("/ok").fetch(4386);
        assertThat(image.bytes()).isEqualTo(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        assertThat(image.contentType()).isEqualTo("image/jpeg");
    }

    /** 扩展名由内容类型决定，而读取端是按对象键后缀判定响应头的，类型必须如实透传。 */
    @Test
    void 内容类型如实透传并剥离参数() {
        assertThat(clientAt("/png").fetch(4386).contentType()).isEqualTo("image/png");
        assertThat(clientAt("/noctype").fetch(4386).contentType()).isEqualTo("image/jpeg");
        // 非 image/* 回落 jpeg，避免把 text/html 当成封面类型写进对象键
        assertThat(clientAt("/htmltype").fetch(4386).contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void 关闭开关时拒绝取图() {
        Wenku8CoverProperties disabled = new Wenku8CoverProperties(false, 5, 10, 2 * 1024 * 1024, 4);
        Wenku8CoverClient client = new Wenku8CoverClient(HttpClient.newHttpClient(), disabled,
            url -> baseUrl + "/ok/" + url);
        assertThatThrownBy(() -> client.fetch(4386))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("未启用");
    }

    @Test
    void 非法aid被拒绝且不发请求() {
        Wenku8CoverClient client = clientAt("/ok");
        assertThatThrownBy(() -> client.fetch(0)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> client.fetch(-1)).isInstanceOf(BusinessException.class);
        assertThat(requests.get()).isZero();
    }

    @Test
    void 超出体积上限被拒绝() {
        assertThatThrownBy(() -> clientAt("/huge", 16).fetch(4386))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("体积上限");
    }

    /**
     * 质询判定必须先于状态码分类：CF 以 403 返回 HTML，若先按状态码分类会被误报为上游异常，
     * 更严重的是可能把这页 HTML 当成图片字节写进对象存储。
     */
    @Test
    void Cloudflare质询页被识别且不重试() {
        assertThatThrownBy(() -> clientAt("/cf").fetch(4386))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Cloudflare");
        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void 限流与未找到映射为对应状态码() {
        assertThatThrownBy(() -> clientAt("/429").fetch(4386))
            .isInstanceOf(BusinessException.class)
            .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(429));
        assertThatThrownBy(() -> clientAt("/missing").fetch(4386))
            .isInstanceOf(BusinessException.class)
            .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(404));
    }

    @Test
    void 空响应体被拒绝() {
        assertThatThrownBy(() -> clientAt("/empty").fetch(4386))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("为空");
    }

    @Test
    void 属性非法值被归一() {
        Wenku8CoverProperties normalized = new Wenku8CoverProperties(true, 0, 0, 0, 0);
        assertThat(normalized.connectTimeoutSeconds()).isEqualTo(5);
        assertThat(normalized.readTimeoutSeconds()).isEqualTo(10);
        assertThat(normalized.maxBytes()).isEqualTo(2 * 1024 * 1024);
        assertThat(normalized.maxConcurrency()).isEqualTo(4);
    }
}
