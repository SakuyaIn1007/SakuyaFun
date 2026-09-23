package com.sakuya.backend.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * CloudflareChallengeTest.java
 * 职责说明：验证质询页判定的特征串命中与边界，确保正文与封面两条直连通道共用同一判定。
 * 执行流程：构造各类响应体 -> 调用 isChallenge -> 断言命中与不命中。
 */
class CloudflareChallengeTest {

    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }

    @Test
    void 四个特征串逐条命中() {
        assertThat(CloudflareChallenge.isChallenge(403, bytes("<title>Just a moment...</title>"))).isTrue();
        assertThat(CloudflareChallenge.isChallenge(503, bytes("<div id=\"cf-error-details\">"))).isTrue();
        assertThat(CloudflareChallenge.isChallenge(403, bytes("Access Denied"))).isTrue();
        assertThat(CloudflareChallenge.isChallenge(503, bytes("used Cloudflare to restrict access"))).isTrue();
    }

    @Test
    void 特征串匹配大小写不敏感() {
        assertThat(CloudflareChallenge.isChallenge(403, bytes("JUST A MOMENT"))).isTrue();
        assertThat(CloudflareChallenge.isChallenge(403, bytes("Cf-Error-Details"))).isTrue();
    }

    /** 只有 403/503 才可能是质询页；其余状态码一律不参与匹配，避免正常报错页被误判。 */
    @Test
    void 非403和503状态码不判定为质询() {
        assertThat(CloudflareChallenge.isChallenge(200, bytes("Just a moment..."))).isFalse();
        assertThat(CloudflareChallenge.isChallenge(404, bytes("Just a moment..."))).isFalse();
        assertThat(CloudflareChallenge.isChallenge(429, bytes("Just a moment..."))).isFalse();
        assertThat(CloudflareChallenge.isChallenge(500, bytes("Just a moment..."))).isFalse();
    }

    @Test
    void 空响应体不判定为质询() {
        assertThat(CloudflareChallenge.isChallenge(403, null)).isFalse();
        assertThat(CloudflareChallenge.isChallenge(503, new byte[0])).isFalse();
    }

    /** 普通图片字节即便以 403 返回，也不应被误判为质询页。 */
    @Test
    void 不含特征串的响应体不判定为质询() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        assertThat(CloudflareChallenge.isChallenge(403, jpeg)).isFalse();
        assertThat(CloudflareChallenge.isChallenge(503, bytes("<html>服务暂时不可用</html>"))).isFalse();
    }

    /** 特征串只可能出现在文档头部；超出扫描窗口的内容不应命中，避免为大响应做全量小写化。 */
    @Test
    void 超出扫描窗口的特征串不命中() {
        byte[] padding = new byte[5000];
        java.util.Arrays.fill(padding, (byte) 'x');
        byte[] body = new byte[padding.length + 20];
        System.arraycopy(padding, 0, body, 0, padding.length);
        System.arraycopy(bytes("just a moment"), 0, body, padding.length, 13);
        assertThat(CloudflareChallenge.isChallenge(403, body)).isFalse();
    }
}
