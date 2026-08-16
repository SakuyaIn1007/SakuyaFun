package com.sakuya.backend.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * NovelRelease.java
 * 职责说明：持久化后台人工确认的轻小说刊发资料，不从 Wenku8 或客户端样例数据推导日期。
 * 执行流程：维护接口写入小说 ID、发售日和卷信息 -> 公共时间表按日期查询 -> 客户端仅展示已维护记录。
 */
@Entity
@Table(name = "novel_releases")
public class NovelRelease {
    @Id private UUID id;
    @Column(nullable = false) private String bookId;
    @Column(nullable = false) private LocalDate releaseDate;
    @Column(nullable = false, length = 160) private String volumeName;
    @Column(nullable = false) private boolean recommended;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected NovelRelease() { }
    public NovelRelease(String bookId, LocalDate releaseDate, String volumeName, boolean recommended) { this.id = UUID.randomUUID(); this.bookId = bookId; this.releaseDate = releaseDate; this.volumeName = volumeName; this.recommended = recommended; this.createdAt = Instant.now(); }
    public UUID getId() { return id; } public String getBookId() { return bookId; } public LocalDate getReleaseDate() { return releaseDate; } public String getVolumeName() { return volumeName; } public boolean isRecommended() { return recommended; }
    public void update(String bookId, LocalDate releaseDate, String volumeName, boolean recommended) { this.bookId = bookId; this.releaseDate = releaseDate; this.volumeName = volumeName; this.recommended = recommended; }
}
