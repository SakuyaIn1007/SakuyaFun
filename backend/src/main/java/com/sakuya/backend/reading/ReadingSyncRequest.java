package com.sakuya.backend.reading;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 客户端一次上传全部待同步变更；空数组表示只拉取当前账号快照。 */
public record ReadingSyncRequest(
    @NotBlank @Size(max = 100) String deviceId,
    @Valid @Size(max = 500) List<ProgressMutation> progressChanges,
    @Valid @Size(max = 1000) List<BookmarkMutation> bookmarkChanges
) {
    public ReadingSyncRequest {
        progressChanges = progressChanges == null ? List.of() : List.copyOf(progressChanges);
        bookmarkChanges = bookmarkChanges == null ? List.of() : List.copyOf(bookmarkChanges);
    }

    public record ProgressMutation(
        @NotBlank @Size(max = 190) String bookId,
        @Size(max = 24) String contentType,
        float progress,
        @Size(max = 190) String chapterId,
        int chapterIndex,
        float chapterProgress,
        @NotNull Instant clientModifiedAt,
        boolean deleted
    ) { }

    public record BookmarkMutation(
        @NotNull UUID id,
        @NotBlank @Size(max = 190) String bookId,
        @Size(max = 160) String title,
        float progress,
        @Size(max = 2000) String note,
        @NotNull Instant createdAt,
        @Size(max = 190) String chapterId,
        float chapterProgress,
        @NotNull Instant clientModifiedAt,
        boolean deleted
    ) { }
}
