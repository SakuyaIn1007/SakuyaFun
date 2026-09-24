package com.sakuya.backend.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * ContentImportState.java
 * 职责说明：记录单本作品按需回填（backfill）的结果，让失败可观测、可重试、不被反复触发。
 * 执行流程：回填开始写入 RUNNING 与尝试次数 -> 成功清理该行或记 SUCCESS -> 失败记录原因与下次可重试时间。
 *
 * 背景：回填原先只把失败写进日志，异常被吞掉后没有任何持久痕迹。
 * 于是同一本「导入必失败」的作品会在每次用户打开时重新整本下载，永远无法收敛。
 */
@Entity
@Table(name = "content_import_states", uniqueConstraints = @UniqueConstraint(
    name = "uk_content_import_state_target", columnNames = {"provider", "external_book_id"}
))
public class ContentImportState {

    /** 连续失败达到该次数后拉长重试间隔，避免对确定性失败的作品持续打上游。 */
    public static final int MAX_ATTEMPTS_WITHOUT_BACKOFF = 3;

    @Id private UUID id;
    @Column(nullable = false, length = 40) private String provider;
    @Column(name = "external_book_id", nullable = false, length = 160) private String externalBookId;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "attempt_count", nullable = false, columnDefinition = "int default 0") private int attemptCount;
    @Lob private String lastError;
    @Column(name = "last_attempt_at") private Instant lastAttemptAt;
    /** 早于该时刻的重试请求会被丢弃；为 null 表示可立即重试。 */
    @Column(name = "next_retry_at") private Instant nextRetryAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ContentImportState() { }

    public ContentImportState(String provider, String externalBookId) {
        this.id = UUID.randomUUID();
        this.provider = provider;
        this.externalBookId = externalBookId;
        this.status = "RUNNING";
        this.attemptCount = 1;
        this.lastAttemptAt = Instant.now();
        this.updatedAt = this.lastAttemptAt;
    }

    public UUID getId() { return id; }
    public String getProvider() { return provider; }
    public String getExternalBookId() { return externalBookId; }
    public String getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public String getLastError() { return lastError; }
    public Instant getNextRetryAt() { return nextRetryAt; }

    public void markRunning() {
        status = "RUNNING";
        attemptCount++;
        lastAttemptAt = Instant.now();
        updatedAt = lastAttemptAt;
    }

    public void markSucceeded() {
        status = "SUCCESS";
        lastError = null;
        nextRetryAt = null;
        updatedAt = Instant.now();
    }

    /**
     * 失败后按尝试次数退避：早期失败快速重试（可能是上游抖动），
     * 连续失败则拉长到小时级，避免确定性失败的作品被反复整本拉取。
     */
    public void markFailed(String error) {
        status = "FAILED";
        lastError = error == null ? "" : error.substring(0, Math.min(2000, error.length()));
        lastAttemptAt = Instant.now();
        updatedAt = lastAttemptAt;
        long delaySeconds = attemptCount <= MAX_ATTEMPTS_WITHOUT_BACKOFF
            ? 60L * attemptCount
            : 3600L;
        nextRetryAt = lastAttemptAt.plusSeconds(delaySeconds);
    }

    /** 未到重试时刻就再次触发，只会重复整本下载，必须提前判断。 */
    public boolean retryAllowedAt(Instant now) {
        return nextRetryAt == null || !now.isBefore(nextRetryAt);
    }
}
