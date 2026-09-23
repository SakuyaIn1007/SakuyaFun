package com.sakuya.backend.content;

import java.nio.charset.StandardCharsets;

/**
 * CloudflareChallenge.java
 * 职责说明：统一 Cloudflare 质询/封禁页的识别特征串，供正文 CDN 与封面 CDN 两条直连通道共用。
 * 执行流程：调用方传入响应状态码与原始字节 -> 本类先按状态码快速排除 -> 再在小写化的前 4KB 中匹配特征串。
 *
 * 抽成共用类而非各自复制一份：两份判定逻辑一旦漂移，会出现「正文拦得住、封面拦不住」的
 * 隐蔽差异——封面侧的表现是把 CF 的 HTML 质询页面当成图片字节写进对象存储，且不报错。
 *
 * 判定必须先于状态码分类：CF 拦截以 403/503 返回 HTML，若先按状态码分类，
 * 会被误报为普通的上游异常/限流，掩盖真实的封禁事实。
 */
public final class CloudflareChallenge {

    private CloudflareChallenge() { }

    /** CF 质询/封禁页只以 403/503 出现；其余状态码一律不参与特征串匹配，避免正常报错页误判。 */
    public static boolean isChallenge(int status, byte[] body) {
        if (status != 403 && status != 503) return false;
        if (body == null || body.length == 0) return false;
        // 特征串只出现在文档头部；限制扫描长度避免为一次判定读完整个大响应。
        String head = new String(body, 0, Math.min(body.length, 4096), StandardCharsets.UTF_8).toLowerCase();
        return head.contains("just a moment") || head.contains("cf-error-details")
            || head.contains("access denied") || head.contains("used cloudflare to restrict access");
    }
}
