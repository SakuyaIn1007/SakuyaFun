package com.sakuya.backend.reading;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 服务端返回当前账号完整有效快照；墓碑只保留在服务端用于拒绝旧写入。 */
public record ReadingSyncResponse(Instant serverTime, List<ProgressDto> progresses, List<BookmarkDto> bookmarks) {
    public record ProgressDto(String bookId, String contentType, float progress, String chapterId, int chapterIndex,
                              float chapterProgress, Instant clientModifiedAt, String deviceId) { }
    public record BookmarkDto(UUID id, String bookId, String title, float progress, String note, Instant createdAt,
                              String chapterId, float chapterProgress, Instant clientModifiedAt, String deviceId) { }
}
