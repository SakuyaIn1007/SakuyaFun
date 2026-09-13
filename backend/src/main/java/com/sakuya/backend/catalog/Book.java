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
    /** 内容库发布状态；已有书目默认保持可见，导入中的新书完成全部文件后再发布。 */
    @Column(nullable = false, columnDefinition = "boolean default true") private boolean published = true;
    @Column(name = "rights_status", nullable = false, length = 24) private String rightsStatus = "AUTHORIZED";
    @Column(name = "license_note", length = 500) private String licenseNote = "";
    @Column(name = "full_content_object_key", length = 512) private String fullContentObjectKey;
    @Column(name = "cover_object_key", length = 512) private String coverObjectKey;
    @Column(name = "full_content_sha256", length = 64) private String fullContentSha256;
    /** 字节数属于基本类型，列必须 NOT NULL；旧库曾以可空列建表，NULL 会让 Hibernate 加载实体时直接抛异常。 */
    @Column(name = "full_content_byte_size", nullable = false, columnDefinition = "bigint default 0") private long fullContentByteSize;
    @Column(name = "cover_sha256", length = 64) private String coverSha256;
    @Column(name = "cover_byte_size", nullable = false, columnDefinition = "bigint default 0") private long coverByteSize;

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

    /** 人工管理和通用 Provider 共用的元数据更新入口，不修改稳定书籍 ID。 */
    public void updateCatalogMetadata(String title, String author, String publisher, float rating,
            List<String> tags, String description, String status) {
        this.title = title; this.author = author; this.publisher = publisher == null ? "" : publisher;
        this.rating = rating; this.tags.clear(); this.tags.addAll(tags == null ? List.of() : tags);
        this.description = description == null ? "" : description; this.status = status;
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
    public boolean isPublished() { return published; }
    public String getRightsStatus() { return rightsStatus == null ? "UNKNOWN" : rightsStatus; }
    public String getLicenseNote() { return licenseNote == null ? "" : licenseNote; }
    public String getFullContentObjectKey() { return fullContentObjectKey; }
    public String getCoverObjectKey() { return coverObjectKey; }
    public String getFullContentSha256() { return fullContentSha256; }
    public long getFullContentByteSize() { return fullContentByteSize; }
    public String getCoverSha256() { return coverSha256; }
    public long getCoverByteSize() { return coverByteSize; }
    /** 管理与导入服务统一更新内容发布信息，版权不明确时不能把正文暴露给客户端。 */
    public void updateContentPublication(boolean published, String rightsStatus, String licenseNote,
            String fullContentObjectKey, String coverObjectKey) {
        this.published = published;
        this.rightsStatus = rightsStatus == null ? "UNKNOWN" : rightsStatus;
        this.licenseNote = licenseNote == null ? "" : licenseNote;
        this.fullContentObjectKey = fullContentObjectKey;
        this.coverObjectKey = coverObjectKey;
    }
    public void updateFullContentObject(String key, String sha256, long byteSize) {
        fullContentObjectKey = key; fullContentSha256 = sha256; fullContentByteSize = byteSize;
    }
    public void updateCoverObject(String key, String sha256, long byteSize) {
        coverObjectKey = key; coverSha256 = sha256; coverByteSize = byteSize;
    }
}
