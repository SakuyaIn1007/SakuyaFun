package com.sakuya.backend.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Wenku8CatalogEntry.java
 * 职责说明：保存某部 Wenku8 小说在指定首页栏目中的位置。
 * 执行流程：同步完成书目 upsert 后，按推荐/轻小说/榜单分别重建本表条目；书目本身不携带单一栏目状态。
 */
@Entity
@Table(name = "wenku8_catalog_entries")
public class Wenku8CatalogEntry {
    @Id private String id;
    @Column(nullable = false) private String feed;
    @Column(nullable = false) private String bookId;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(nullable = false) private Instant syncedAt;

    protected Wenku8CatalogEntry() { }
    public Wenku8CatalogEntry(Wenku8CatalogFeed feed, String bookId, int displayOrder, Instant syncedAt) {
        this.id = feed.name() + ":" + bookId;
        this.feed = feed.name();
        this.bookId = bookId;
        this.displayOrder = displayOrder;
        this.syncedAt = syncedAt;
    }
    public String getBookId() { return bookId; }
    public int getDisplayOrder() { return displayOrder; }
}
