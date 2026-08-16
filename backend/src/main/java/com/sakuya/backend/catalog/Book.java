package com.sakuya.backend.catalog;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Book.java
 * 职责说明：保存本地书目和 Wenku8 小说的基础资料；不保存第三方章节正文。
 * 执行流程：本地书籍沿用原有字段；同步任务以 wenku8-原始ID 定位远端书籍并覆盖上游可变资料，
 * 栏目归属则由 Wenku8CatalogEntry 单独维护，避免同一本书在多个榜单中互相覆盖。
 */
@Entity
@Table(name = "books")
public class Book {
    @Id private String id;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String author;
    /** 本地书籍的出版社；Wenku8 未提供此信息时使用空串，不能把来源错误写成出版社。 */
    @Column(nullable = false) private String publisher;
    private float rating;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "book_tags", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "tag") private Set<String> tags = new LinkedHashSet<>();
    @Lob private String description;
    /** 旧栏目字段仅为已存在本地书目兼容保留；Wenku8 栏目请查询 Wenku8CatalogEntry。 */
    private String category;

    @Column(name = "source_type") private String sourceType = "LOCAL";
    @Column(name = "source_novel_id") private String sourceNovelId;
    private String status;
    private boolean copyrightRestricted;
    private String coverPath;
    private Instant sourceSyncedAt;

    protected Book() { }

    public Book(String id, String title, String author, String publisher, float rating, List<String> tags, String description, String category) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.rating = rating;
        this.tags.addAll(tags);
        this.description = description;
        this.category = category;
    }

    /** 同步时只使用 Wenku8 实际提供的资料；detail 为 null 时保留旧的完整简介，避免局部失败降级数据。 */
    public void refreshFromWenku8(String aid, String title, String author, List<String> tags, String listDescription,
            String status, boolean copyrightRestricted, String detailDescription, Instant syncedAt) {
        this.sourceType = "WENKU8";
        this.sourceNovelId = aid;
        this.title = title;
        this.author = author;
        this.publisher = "";
        this.rating = 0f;
        this.tags.clear();
        this.tags.addAll(tags);
        this.status = status;
        this.copyrightRestricted = copyrightRestricted;
        this.coverPath = "/wenku8/novels/" + aid + "/cover";
        if (detailDescription != null && !detailDescription.isBlank()) this.description = detailDescription;
        else if (this.description == null || this.description.isBlank()) this.description = listDescription;
        this.sourceSyncedAt = syncedAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher == null ? "" : publisher; }
    public float getRating() { return rating; }
    public Set<String> getTags() { return tags; }
    public String getDescription() { return description == null ? "" : description; }
    public String getCategory() { return category; }
    public String getSourceType() { return sourceType == null ? "LOCAL" : sourceType; }
    public String getSourceNovelId() { return sourceNovelId; }
    public String getStatus() { return status == null ? "" : status; }
    public boolean isCopyrightRestricted() { return copyrightRestricted; }
    public String getCoverPath() { return coverPath; }
    public Instant getSourceSyncedAt() { return sourceSyncedAt; }
}
