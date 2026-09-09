package com.sakuya.backend.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * ContentSourceMapping.java
 * 职责说明：把可替换数据源的外部作品 ID 映射到稳定的内部 bookId。
 * 执行流程：导入前按 provider + externalBookId 查找映射；管理员也可把新来源绑定到已有书籍。
 */
@Entity
@Table(name = "content_source_mappings", uniqueConstraints = @UniqueConstraint(
    name = "uk_content_source_external", columnNames = {"provider", "external_book_id"}
))
public class ContentSourceMapping {
    @Id private UUID id;
    @Column(nullable = false, length = 40) private String provider;
    @Column(name = "external_book_id", nullable = false, length = 160) private String externalBookId;
    @Column(name = "book_id", nullable = false) private String bookId;
    @Column(name = "last_synced_at") private Instant lastSyncedAt;

    protected ContentSourceMapping() { }
    public ContentSourceMapping(String provider, String externalBookId, String bookId) {
        this.id = UUID.randomUUID(); this.provider = provider; this.externalBookId = externalBookId; this.bookId = bookId;
    }
    public String getProvider() { return provider; }
    public String getExternalBookId() { return externalBookId; }
    public String getBookId() { return bookId; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void markSynced(Instant value) { lastSyncedAt = value; }
}
