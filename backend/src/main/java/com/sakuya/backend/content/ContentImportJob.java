package com.sakuya.backend.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * ContentImportJob.java
 * 职责说明：记录后台导入任务的生命周期和可诊断结果。
 * 执行流程：管理 API 创建 PENDING 任务 -> 异步执行器标记 RUNNING -> 完成后写 SUCCESS 或 FAILED。
 */
@Entity
@Table(name = "content_import_jobs")
public class ContentImportJob {
    @Id private UUID id;
    @Column(nullable = false, length = 40) private String provider;
    @Column(nullable = false, length = 20) private String mode;
    @Column(nullable = false, length = 20) private String status;
    private int successCount;
    private int failureCount;
    @Lob private String errorMessage;
    @Column(nullable = false) private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

    protected ContentImportJob() { }
    public ContentImportJob(String provider, String mode) {
        this.id = UUID.randomUUID(); this.provider = provider; this.mode = mode;
        this.status = "PENDING"; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getProvider() { return provider; }
    public String getMode() { return mode; }
    public String getStatus() { return status; }
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void start() { status = "RUNNING"; startedAt = Instant.now(); }
    public void complete(int success, int failure, String error) {
        successCount = success; failureCount = failure; errorMessage = error;
        status = failure == 0 ? "SUCCESS" : success > 0 ? "PARTIAL" : "FAILED"; finishedAt = Instant.now();
    }
}
