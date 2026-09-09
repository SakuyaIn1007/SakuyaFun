package com.sakuya.backend.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** 通用栏目条目；来源导入和人工维护都写入本表，客户端不再感知 Wenku8 专用栏目。 */
@Entity
@Table(name = "content_catalog_entries", indexes = @Index(name = "idx_content_catalog_feed_order", columnList = "feed,display_order"))
public class ContentCatalogEntry {
    @Id private String id;
    @Column(nullable = false, length = 40) private String feed;
    @Column(name = "book_id", nullable = false) private String bookId;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(nullable = false) private Instant updatedAt;

    protected ContentCatalogEntry() { }
    public ContentCatalogEntry(String feed, String bookId, int displayOrder, Instant updatedAt) {
        this.id = feed + ":" + bookId; this.feed = feed; this.bookId = bookId;
        this.displayOrder = displayOrder; this.updatedAt = updatedAt;
    }
    public String getBookId() { return bookId; }
    public int getDisplayOrder() { return displayOrder; }
}
