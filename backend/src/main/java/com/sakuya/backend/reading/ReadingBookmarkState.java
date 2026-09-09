package com.sakuya.backend.reading;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * ReadingBookmarkState.java
 * 职责说明：保存账号级在线书签及删除墓碑，书签 UUID 只在所属用户命名空间内唯一。
 * 执行流程：客户端先本地生成 UUID -> 同步服务校验书籍和章节 -> 按冲突顺序幂等写入。
 */
@Entity
@Table(name = "reading_bookmark_states", indexes = {
    @Index(name = "idx_reading_bookmark_user", columnList = "user_id"),
    @Index(name = "idx_reading_bookmark_user_book", columnList = "user_id,book_id")
})
public class ReadingBookmarkState {
    @EmbeddedId private Id id;
    @Column(name = "book_id", nullable = false, length = 190) private String bookId;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false) private float progress;
    @Column(nullable = false, length = 2000) private String note;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "chapter_id", length = 190) private String chapterId;
    @Column(name = "chapter_progress", nullable = false) private float chapterProgress;
    @Column(name = "client_modified_at", nullable = false) private Instant clientModifiedAt;
    @Column(name = "device_id", nullable = false, length = 100) private String deviceId;
    @Column(nullable = false) private boolean deleted;
    @Column(name = "server_updated_at", nullable = false) private Instant serverUpdatedAt;

    protected ReadingBookmarkState() { }

    public ReadingBookmarkState(UUID userId, UUID bookmarkId, String bookId, String title, float progress, String note,
            Instant createdAt, String chapterId, float chapterProgress, Instant clientModifiedAt, String deviceId, boolean deleted) {
        this.id = new Id(userId, bookmarkId);
        apply(bookId, title, progress, note, createdAt, chapterId, chapterProgress, clientModifiedAt, deviceId, deleted);
    }

    public void apply(String bookId, String title, float progress, String note, Instant createdAt, String chapterId,
            float chapterProgress, Instant clientModifiedAt, String deviceId, boolean deleted) {
        this.bookId = bookId;
        this.title = title;
        this.progress = progress;
        this.note = note;
        this.createdAt = createdAt;
        this.chapterId = chapterId;
        this.chapterProgress = chapterProgress;
        this.clientModifiedAt = clientModifiedAt;
        this.deviceId = deviceId;
        this.deleted = deleted;
        this.serverUpdatedAt = Instant.now();
    }

    public boolean isOlderThan(Instant modifiedAt, String candidateDeviceId) {
        int time = clientModifiedAt.compareTo(modifiedAt);
        return time < 0 || (time == 0 && deviceId.compareTo(candidateDeviceId) < 0);
    }

    public UUID getUserId() { return id.userId; }
    public UUID getBookmarkId() { return id.bookmarkId; }
    public String getBookId() { return bookId; }
    public String getTitle() { return title; }
    public float getProgress() { return progress; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
    public String getChapterId() { return chapterId; }
    public float getChapterProgress() { return chapterProgress; }
    public Instant getClientModifiedAt() { return clientModifiedAt; }
    public String getDeviceId() { return deviceId; }
    public boolean isDeleted() { return deleted; }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "user_id", nullable = false) private UUID userId;
        @Column(name = "bookmark_id", nullable = false) private UUID bookmarkId;
        protected Id() { }
        Id(UUID userId, UUID bookmarkId) { this.userId = userId; this.bookmarkId = bookmarkId; }
        @Override public boolean equals(Object other) {
            return other instanceof Id value && Objects.equals(userId, value.userId) && Objects.equals(bookmarkId, value.bookmarkId);
        }
        @Override public int hashCode() { return Objects.hash(userId, bookmarkId); }
    }
}
