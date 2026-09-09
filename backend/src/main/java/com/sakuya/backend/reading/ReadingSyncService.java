package com.sakuya.backend.reading;

import com.sakuya.backend.common.BusinessException;
import com.sakuya.backend.content.ContentReadService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ReadingSyncService.java
 * 职责说明：协调账号级阅读进度、书签的校验、冲突合并和完整快照返回。
 * 执行流程：规范化客户端时间 -> 校验稳定书籍及可选章节 -> 按 (修改时间, deviceId) 应用较新变更
 * -> 在同一事务中返回当前用户的有效状态；服务端墓碑阻止旧设备复活删除记录。
 */
@Service
public class ReadingSyncService {
    private final ReadingProgressStateRepository progresses;
    private final ReadingBookmarkStateRepository bookmarks;
    private final ContentReadService content;

    public ReadingSyncService(ReadingProgressStateRepository progresses, ReadingBookmarkStateRepository bookmarks,
            ContentReadService content) {
        this.progresses = progresses; this.bookmarks = bookmarks; this.content = content;
    }

    @Transactional
    public ReadingSyncResponse sync(UUID userId, ReadingSyncRequest request) {
        for (var mutation : request.progressChanges()) applyProgress(userId, request.deviceId(), mutation);
        for (var mutation : request.bookmarkChanges()) applyBookmark(userId, request.deviceId(), mutation);
        return snapshot(userId);
    }

    private void applyProgress(UUID userId, String deviceId, ReadingSyncRequest.ProgressMutation mutation) {
        String bookId = canonicalBook(mutation.bookId());
        validatePosition(mutation.progress(), mutation.chapterIndex(), mutation.chapterProgress());
        validateChapter(bookId, mutation.chapterId());
        Instant modifiedAt = normalizeTime(mutation.clientModifiedAt());
        ReadingProgressState existing = progresses.findByIdUserIdAndIdBookId(userId, bookId).orElse(null);
        if (existing != null && !existing.isOlderThan(modifiedAt, deviceId)) return;
        String type = mutation.contentType() == null || mutation.contentType().isBlank() ? "WENKU8" : mutation.contentType();
        if (existing == null) {
            progresses.save(new ReadingProgressState(userId, bookId, type, mutation.progress(), mutation.chapterId(),
                mutation.chapterIndex(), mutation.chapterProgress(), modifiedAt, deviceId, mutation.deleted()));
        } else {
            existing.apply(type, mutation.progress(), mutation.chapterId(), mutation.chapterIndex(), mutation.chapterProgress(),
                modifiedAt, deviceId, mutation.deleted());
        }
    }

    private void applyBookmark(UUID userId, String deviceId, ReadingSyncRequest.BookmarkMutation mutation) {
        String bookId = canonicalBook(mutation.bookId());
        validatePosition(mutation.progress(), 0, mutation.chapterProgress());
        validateChapter(bookId, mutation.chapterId());
        Instant modifiedAt = normalizeTime(mutation.clientModifiedAt());
        ReadingBookmarkState existing = bookmarks.findByIdUserIdAndIdBookmarkId(userId, mutation.id()).orElse(null);
        if (existing != null && !existing.isOlderThan(modifiedAt, deviceId)) return;
        String title = mutation.title() == null || mutation.title().isBlank() ? "阅读书签" : mutation.title().trim();
        String note = mutation.note() == null ? "" : mutation.note();
        if (existing == null) {
            bookmarks.save(new ReadingBookmarkState(userId, mutation.id(), bookId, title, mutation.progress(), note,
                mutation.createdAt(), mutation.chapterId(), mutation.chapterProgress(), modifiedAt, deviceId, mutation.deleted()));
        } else {
            existing.apply(bookId, title, mutation.progress(), note, mutation.createdAt(), mutation.chapterId(),
                mutation.chapterProgress(), modifiedAt, deviceId, mutation.deleted());
        }
    }

    private ReadingSyncResponse snapshot(UUID userId) {
        var progressDtos = progresses.findByIdUserId(userId).stream().filter(state -> !state.isDeleted())
            .map(state -> new ReadingSyncResponse.ProgressDto(state.getBookId(), state.getContentType(), state.getProgress(),
                state.getChapterId(), state.getChapterIndex(), state.getChapterProgress(), state.getClientModifiedAt(), state.getDeviceId()))
            .toList();
        var bookmarkDtos = bookmarks.findByIdUserId(userId).stream().filter(state -> !state.isDeleted())
            .map(state -> new ReadingSyncResponse.BookmarkDto(state.getBookmarkId(), state.getBookId(), state.getTitle(),
                state.getProgress(), state.getNote(), state.getCreatedAt(), state.getChapterId(), state.getChapterProgress(),
                state.getClientModifiedAt(), state.getDeviceId()))
            .toList();
        return new ReadingSyncResponse(Instant.now(), progressDtos, bookmarkDtos);
    }

    private String canonicalBook(String requestedBookId) {
        return content.resolveBookId(requestedBookId.startsWith("content:") ? requestedBookId.substring("content:".length()) : requestedBookId);
    }

    private void validateChapter(String bookId, String chapterId) {
        if (chapterId == null || chapterId.isBlank()) return;
        boolean belongs = content.chapterIndex(bookId).volumes().stream().flatMap(volume -> volume.chapters().stream())
            .anyMatch(chapter -> chapter.id().equals(chapterId));
        if (!belongs) throw new BusinessException(400, "章节不属于指定图书");
    }

    private void validatePosition(float progress, int chapterIndex, float chapterProgress) {
        if (!Float.isFinite(progress) || progress < 0f || progress > 1f || chapterIndex < 0
                || !Float.isFinite(chapterProgress) || chapterProgress < 0f || chapterProgress > 1f) {
            throw new BusinessException(400, "阅读位置超出有效范围");
        }
    }

    private Instant normalizeTime(Instant value) {
        Instant now = Instant.now();
        return value.isAfter(now.plus(5, ChronoUnit.MINUTES)) ? now : value;
    }
}
