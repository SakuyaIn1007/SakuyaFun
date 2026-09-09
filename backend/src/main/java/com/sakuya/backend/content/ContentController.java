package com.sakuya.backend.content;

import com.sakuya.backend.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 来源无关的客户端内容 API；所有正常请求只读取后端内容库。 */
@Validated
@RestController
@RequestMapping("/content")
public class ContentController {
    private final ContentFallbackFacade service;
    public ContentController(ContentFallbackFacade service) { this.service = service; }
    @GetMapping("/novels") public ApiResponse<Object> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize) {
        return ApiResponse.ok(service.search(keyword, page, pageSize));
    }
    @GetMapping("/novels/{bookId}") public ApiResponse<Object> novel(@PathVariable String bookId) { return ApiResponse.ok(service.novel(bookId)); }
    @GetMapping("/novels/{bookId}/chapters") public ApiResponse<Object> chapters(@PathVariable String bookId) { return ApiResponse.ok(service.chapters(bookId)); }
    @GetMapping("/chapters/{chapterId}") public ApiResponse<Object> chapter(@PathVariable String chapterId,
            @RequestParam(required = false) String bookId) { return ApiResponse.ok(service.chapter(chapterId, bookId)); }
    @GetMapping("/novels/{bookId}/full-content") public ApiResponse<Object> fullContent(@PathVariable String bookId) { return ApiResponse.ok(service.fullContent(bookId)); }
    @GetMapping("/novels/{bookId}/cover") public void cover(@PathVariable String bookId, HttpServletResponse response) throws IOException {
        ContentReadService.CoverDto cover = service.cover(bookId);
        response.setContentType(cover.contentType()); response.getOutputStream().write(cover.bytes());
    }
}
