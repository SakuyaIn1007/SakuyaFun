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
 * ReadingProgressState.java
 * 职责说明：保存单个用户、单本在线书的云端阅读位置和删除墓碑。
 * 执行流程：同步服务完成内容归属校验 -> 比较客户端修改时间与设备 ID -> 覆盖较新的状态；
 * 删除只写 deleted=true，避免长期离线设备用旧进度重新复活记录。
 */
@Entity
@Table(name = "reading_progress_states", indexes = @Index(name = "idx_reading_progress_user", columnList = "user_id"))
public class ReadingProgressState {
    @EmbeddedId private Id id;
    @Column(name = "content_type", nullable = false, length = 24) private String contentType;
    @Column(nullable = false) private float progress;
    @Column(name = "chapter_id", length = 190) private String chapterId;
    @Column(name = "chapter_index", nullable = false) private int chapterIndex;
    @Column(name = "chapter_progress", nullable = false) private float chapterProgress;
    @Column(name = "client_modified_at", nullable = false) private Instant clientModifiedAt;
    @Column(name = "device_id", nullable = false, length = 100) private String deviceId;
    @Column(nullable = false) private boolean deleted;
    @Column(name = "server_updated_at", nullable = false) private Instant serverUpdatedAt;

    protected ReadingProgressState() { }

    public ReadingProgressState(UUID userId, String bookId, String contentType, float progress, String chapterId,
            int chapterIndex, float chapterProgress, Instant clientModifiedAt, String deviceId, boolean deleted) {
        this.id = new Id(userId, bookId);
        apply(contentType, progress, chapterId, chapterIndex, chapterProgress, clientModifiedAt, deviceId, deleted);
    }

    public void apply(String contentType, float progress, String chapterId, int chapterIndex, float chapterProgress,
            Instant clientModifiedAt, String deviceId, boolean deleted) {
        this.contentType = contentType;
        this.progress = progress;
        this.chapterId = chapterId;
        this.chapterIndex = chapterIndex;
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
    public String getBookId() { return id.bookId; }
    public String getContentType() { return contentType; }
    public float getProgress() { return progress; }
    public String getChapterId() { return chapterId; }
    public int getChapterIndex() { return chapterIndex; }
    public float getChapterProgress() { return chapterProgress; }
    public Instant getClientModifiedAt() { return clientModifiedAt; }
    public String getDeviceId() { return deviceId; }
    public boolean isDeleted() { return deleted; }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "user_id", nullable = false) private UUID userId;
        @Column(name = "book_id", nullable = false, length = 190) private String bookId;
        protected Id() { }
        Id(UUID userId, String bookId) { this.userId = userId; this.bookId = bookId; }
        @Override public boolean equals(Object other) {
            return other instanceof Id value && Objects.equals(userId, value.userId) && Objects.equals(bookId, value.bookId);
        }
        @Override public int hashCode() { return Objects.hash(userId, bookId); }
    }
}
