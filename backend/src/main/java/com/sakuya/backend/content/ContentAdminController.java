package com.sakuya.backend.content;

import com.sakuya.backend.catalog.Book;
import com.sakuya.backend.common.ApiResponse;
import com.sakuya.backend.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * ContentAdminController.java
 * 职责说明：提供第一阶段无网页的受保护内容管理 API。
 * 执行流程：每个请求先验证部署令牌 -> 校验 DTO/文件 -> 委托 AdminService 或导入协调器。
 */
@RestController
@RequestMapping("/admin/content")
public class ContentAdminController {
    private final ContentManagementProperties properties;
    private final ContentAdminService admin;
    private final ContentImportCoordinator imports;
    public ContentAdminController(ContentManagementProperties properties, ContentAdminService admin, ContentImportCoordinator imports) {
        this.properties = properties; this.admin = admin; this.imports = imports;
    }
    @PostMapping("/books") public ApiResponse<BookResult> create(@RequestHeader(value = "X-Content-Management-Token", required = false) String token,
            @Valid @RequestBody BookRequest request) { authorize(token); return ApiResponse.ok(result(admin.saveBook(null, request.title(), request.author(), request.publisher(), request.rating(), request.tags(), request.description(), request.status(), request.rightsStatus(), request.licenseNote(), request.published()))); }
    @PutMapping("/books/{bookId}") public ApiResponse<BookResult> update(@RequestHeader(value = "X-Content-Management-Token", required = false) String token,
            @PathVariable String bookId, @Valid @RequestBody BookRequest request) { authorize(token); return ApiResponse.ok(result(admin.saveBook(bookId, request.title(), request.author(), request.publisher(), request.rating(), request.tags(), request.description(), request.status(), request.rightsStatus(), request.licenseNote(), request.published()))); }
    @PostMapping(value = "/books/{bookId}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> document(@RequestHeader(value = "X-Content-Management-Token", required = false) String token,
            @PathVariable String bookId, @RequestPart("file") MultipartFile file) throws java.io.IOException {
        authorize(token); if (file.isEmpty()) throw new BusinessException(400, "请选择 TXT 或 EPUB 文件");
        admin.uploadDocument(bookId, file.getOriginalFilename(), file.getBytes()); return ApiResponse.ok();
    }
    @PostMapping(value = "/books/{bookId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> cover(@RequestHeader(value = "X-Content-Management-Token", required = false) String token,
            @PathVariable String bookId, @RequestPart("file") MultipartFile file) throws java.io.IOException {
        authorize(token); if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) throw new BusinessException(400, "请选择封面图片");
        admin.uploadCover(bookId, file.getBytes(), file.getContentType()); return ApiResponse.ok();
    }
    @PutMapping("/feeds/{feed}") public ApiResponse<Void> feed(@RequestHeader(value = "X-Content-Management-Token", required = false) String token,
            @PathVariable String feed, @RequestBody @NotEmpty List<String> bookIds) { authorize(token); admin.replaceFeed(feed, bookIds); return ApiResponse.ok(); }
    @PostMapping("/source-mappings") public ApiResponse<Void> mapping(@RequestHeader(value = "X-Content-Management-Token", required = false) String token,
            @Valid @RequestBody SourceMappingRequest request) { authorize(token); admin.bindSource(request.provider(), request.externalBookId(), request.bookId()); return ApiResponse.ok(); }
    @PostMapping("/imports") public ApiResponse<ImportResult> startImport(@RequestHeader(value = "X-Content-Management-Token", required = false) String token,
            @Valid @RequestBody ImportRequest request) { authorize(token); return ApiResponse.ok(new ImportResult(imports.create(request.provider(), request.mode()))); }
    @GetMapping("/imports/{jobId}") public ApiResponse<ImportJobResult> importStatus(@RequestHeader(value = "X-Content-Management-Token", required = false) String token,
            @PathVariable UUID jobId) { authorize(token); ContentImportJob job = imports.get(jobId); return ApiResponse.ok(new ImportJobResult(job.getId(), job.getProvider(), job.getMode(), job.getStatus(), job.getSuccessCount(), job.getFailureCount(), job.getErrorMessage())); }

    private void authorize(String token) {
        String expected = properties.token();
        boolean valid = expected != null && !expected.isBlank() && token != null && MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
        if (!valid) throw new BusinessException(403, "内容管理权限不足");
    }
    private BookResult result(Book book) { return new BookResult(book.getId(), book.getTitle(), book.isPublished(), book.getRightsStatus()); }
    public record BookRequest(@NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 120) String author,
        @Size(max = 120) String publisher, float rating, List<String> tags, @Size(max = 10000) String description,
        // rightsStatus 已退化为展示字段，不再参与任何判定，因此不强制填写。
        @Size(max = 80) String status, @Size(max = 24) String rightsStatus, @Size(max = 500) String licenseNote, boolean published) { }
    public record SourceMappingRequest(@NotBlank String provider, @NotBlank String externalBookId, @NotBlank String bookId) { }
    public record ImportRequest(@NotBlank String provider, String mode) { }
    public record BookResult(String id, String title, boolean published, String rightsStatus) { }
    public record ImportResult(UUID jobId) { }
    public record ImportJobResult(UUID jobId, String provider, String mode, String status, int successCount, int failureCount, String errorMessage) { }
}
