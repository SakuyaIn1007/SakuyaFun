package com.sakuya.backend.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * ContentChapter.java
 * 职责说明：保存章节结构、正文对象引用和完整性信息，不把大正文写入 MySQL。
 * 执行流程：导入器先写 ContentStorage -> 校验哈希 -> 在同一数据库事务中发布章节索引。
 */
@Entity
@Table(name = "content_chapters", indexes = {
    @Index(name = "idx_content_chapter_book_order", columnList = "book_id,display_order"),
    @Index(name = "idx_content_chapter_volume_order", columnList = "volume_id,display_order")
})
public class ContentChapter {
    @Id private String id;
    @Column(name = "book_id", nullable = false) private String bookId;
    @Column(name = "volume_id", nullable = false) private String volumeId;
    @Column(name = "source_chapter_id", length = 160) private String sourceChapterId;
    @Column(nullable = false) private String title;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "content_object_key", nullable = false, length = 512) private String contentObjectKey;
    @Column(nullable = false, length = 64) private String sha256;
    @Column(name = "byte_size", nullable = false) private long byteSize;
    @Column(name = "full_text_offset", nullable = false) private int fullTextOffset;
    @Column(nullable = false) private Instant updatedAt;

    protected ContentChapter() { }
    public ContentChapter(String id, String bookId, String volumeId, String sourceChapterId, String title, int displayOrder,
            String contentObjectKey, String sha256, long byteSize, int fullTextOffset, Instant updatedAt) {
        this.id = id; this.bookId = bookId; this.volumeId = volumeId; this.sourceChapterId = sourceChapterId; this.title = title;
        this.displayOrder = displayOrder; this.contentObjectKey = contentObjectKey; this.sha256 = sha256;
        this.byteSize = byteSize; this.fullTextOffset = fullTextOffset; this.updatedAt = updatedAt;
    }
    public String getId() { return id; }
    public String getBookId() { return bookId; }
    public String getVolumeId() { return volumeId; }
    public String getSourceChapterId() { return sourceChapterId; }
    public String getTitle() { return title; }
    public int getDisplayOrder() { return displayOrder; }
    public String getContentObjectKey() { return contentObjectKey; }
    public String getSha256() { return sha256; }
    public long getByteSize() { return byteSize; }
    public int getFullTextOffset() { return fullTextOffset; }
}
