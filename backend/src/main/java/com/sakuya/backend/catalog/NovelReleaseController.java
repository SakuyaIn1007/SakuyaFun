package com.sakuya.backend.catalog;

import com.sakuya.backend.common.ApiResponse;
import com.sakuya.backend.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * NovelReleaseController.java
 * 职责说明：公开已维护时间表，并提供给受部署令牌保护的后台 CRUD 接口。
 * 执行流程：普通用户读取 /novels/releases -> 维护者携带 X-Release-Management-Token 写入 -> Service 持久化记录。
 */
@RestController
public class NovelReleaseController {
    private final NovelReleaseService service; private final BookRepository books; private final ReleaseManagementProperties properties;
    public NovelReleaseController(NovelReleaseService service, BookRepository books, ReleaseManagementProperties properties) { this.service = service; this.books = books; this.properties = properties; }
    @GetMapping("/novels/releases") public ApiResponse<List<ReleaseDto>> list() { return ApiResponse.ok(service.list().stream().map(this::dto).toList()); }
    @PostMapping("/admin/novel-releases") public ApiResponse<ReleaseDto> create(@RequestHeader(value = "X-Release-Management-Token", required = false) String token, @Valid @RequestBody ReleaseRequest request) { authorize(token); return ApiResponse.ok(dto(service.create(request.bookId(), request.releaseDate(), request.volumeName(), request.recommended()))); }
    @PutMapping("/admin/novel-releases/{id}") public ApiResponse<ReleaseDto> update(@RequestHeader(value = "X-Release-Management-Token", required = false) String token, @PathVariable UUID id, @Valid @RequestBody ReleaseRequest request) { authorize(token); return ApiResponse.ok(dto(service.update(id, request.bookId(), request.releaseDate(), request.volumeName(), request.recommended()))); }
    @DeleteMapping("/admin/novel-releases/{id}") public ApiResponse<Void> delete(@RequestHeader(value = "X-Release-Management-Token", required = false) String token, @PathVariable UUID id) { authorize(token); service.delete(id); return ApiResponse.ok(); }
    private void authorize(String token) { String expected = properties.token(); if (expected == null || expected.isBlank() || !expected.equals(token)) throw new BusinessException(403, "时间表维护权限不足"); }
    private ReleaseDto dto(NovelRelease release) { Book book = books.findById(release.getBookId()).orElseThrow(() -> new BusinessException(404, "关联书目不存在")); return new ReleaseDto(release.getId(), new CatalogController.ContentItem(book.getId(), book.getTitle(), book.getAuthor(), book.getPublisher(), book.getRating(), List.copyOf(book.getTags()), book.getDescription(), false, book.getSourceType(), book.getSourceNovelId(), book.getCoverPath(), book.getStatus(), book.isCopyrightRestricted()), release.getReleaseDate().toString(), release.getVolumeName(), release.isRecommended()); }
    public record ReleaseRequest(@NotBlank String bookId, @NotNull LocalDate releaseDate, @NotBlank @Size(max = 160) String volumeName, boolean recommended) { }
    public record ReleaseDto(UUID id, CatalogController.ContentItem novel, String releaseDate, String volumeName, boolean isRecommended) { }
}
