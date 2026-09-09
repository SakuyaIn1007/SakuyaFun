package com.sakuya.backend.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * ContentVolume.java
 * 职责说明：保存统一内容库中的卷信息，卷 ID 不依赖任何上游站点。
 * 执行流程：导入器按来源目录生成稳定 ID -> 原子替换书籍目录 -> 客户端按 displayOrder 读取。
 */
@Entity
@Table(name = "content_volumes", indexes = @Index(name = "idx_content_volume_book_order", columnList = "book_id,display_order"))
public class ContentVolume {
    @Id private String id;
    @Column(name = "book_id", nullable = false) private String bookId;
    @Column(nullable = false) private String title;
    @Column(name = "display_order", nullable = false) private int displayOrder;

    protected ContentVolume() { }
    public ContentVolume(String id, String bookId, String title, int displayOrder) {
        this.id = id; this.bookId = bookId; this.title = title; this.displayOrder = displayOrder;
    }
    public String getId() { return id; }
    public String getBookId() { return bookId; }
    public String getTitle() { return title; }
    public int getDisplayOrder() { return displayOrder; }
}
