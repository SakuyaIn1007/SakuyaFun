package com.sakuya.backend.wenku8;

import com.sakuya.backend.common.ApiResponse;
import com.sakuya.backend.content.ContentFallbackFacade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.IOException;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wenku8Controller.java
 * 职责说明：提供已认证客户端访问的 Wenku8 搜索、详情、目录、分章正文与连续全文网关接口。
 * 执行流程：JWT 安全链先认证 -> Controller 校验请求 -> GatewayService 转发内网适配服务 -> ApiResponse 统一输出。
 */
@Validated @RestController @RequestMapping("/wenku8")
public class Wenku8Controller {
    private final ContentFallbackFacade content;
    public Wenku8Controller(ContentFallbackFacade content) { this.content = content; }
    @GetMapping("/novels/search") public ApiResponse<Object> search(@RequestParam @NotBlank String keyword, @RequestParam(defaultValue = "0") @Min(0) @Max(100) int page) { return ApiResponse.ok(content.search(keyword, page, 20)); }
    @GetMapping("/novels/{id}") public ApiResponse<Object> novel(@PathVariable String id) { return ApiResponse.ok(content.novel(id)); }
    @GetMapping("/novels/list") public ApiResponse<Object> list(@RequestParam(defaultValue = "lastupdate") String sort) { return ApiResponse.ok(content.search("", 0, 20)); }
    @GetMapping("/novels/{id}/chapters") public ApiResponse<Object> chapters(@PathVariable String id) { return ApiResponse.ok(content.chapters(id)); }
    /** 全文响应可能较大；服务层使用大响应超时和流式解析，Controller 只负责认证后的 DTO 输出。 */
    @GetMapping("/novels/{id}/full-content") public ApiResponse<Object> fullContent(@PathVariable String id) { return ApiResponse.ok(content.fullContent(id)); }
    @GetMapping("/chapters/{chapterId}/content") public ApiResponse<Object> content(@RequestParam @NotBlank String novelId, @PathVariable String chapterId) { return ApiResponse.ok(content.chapter(chapterId, novelId)); }
    /** Android 使用同一受认证网关加载封面；后端代理字节流，内部适配服务地址不会暴露给客户端。 */
    @GetMapping("/novels/{id}/cover") public void cover(@PathVariable String id, HttpServletResponse response) throws IOException, InterruptedException {
        var cover = content.cover(id); response.setContentType(cover.contentType()); response.getOutputStream().write(cover.bytes());
    }
}
