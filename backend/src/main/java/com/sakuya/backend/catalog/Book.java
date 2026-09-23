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
    /**
     * 详情是否已补全。null = 当前 description 来自列表页的截断预览，待补抓；
     * 非空 = 已成功取得过详情响应，不再重复抓取。
     *
     * 这个字段是「同步阶段零详情请求」成立的必要条件：目录同步与内容读取读写的是同一张 books 表，
     * 同步写入后 database.novel() 必然命中、永不 404，回退门面因此永远不会触发详情回退。
     * 若没有该标记，截断的 intro_preview 会被**永久固化**，用户点开详情页也拿不到完整简介。
     */
    @Column(name = "detail_synced_at") private Instant detailSyncedAt;
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

    /**
     * 同步时只使用 Wenku8 实际提供的资料。
     *
     * detailDescription 恒为 null（目录同步已不再请求详情），因此当现存 description 非空时不会覆盖——
     * 这带来一个**已确认接受**的行为变化：已补全的简介不再随每日同步更新，上游改简介后本地不跟随。
     * 换取的是同步阶段零详情请求；如需修正可对超期书籍重新补抓，本设计不引入周期性重抓。
     */
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
    public Instant getDetailSyncedAt() { return detailSyncedAt; }
    public boolean isPublished() { return published; }
    public String getRightsStatus() { return rightsStatus == null ? "UNKNOWN" : rightsStatus; }
    public String getLicenseNote() { return licenseNote == null ? "" : licenseNote; }
    public String getFullContentObjectKey() { return fullContentObjectKey; }
    public String getCoverObjectKey() { return coverObjectKey; }
    public String getFullContentSha256() { return fullContentSha256; }
    public long getFullContentByteSize() { return fullContentByteSize; }
    public String getCoverSha256() { return coverSha256; }
    public long getCoverByteSize() { return coverByteSize; }
    /**
     * 管理与导入服务统一更新内容发布信息。
     * published 是唯一的读取开关；rightsStatus 仅作展示与后台标注，不参与任何判定，
     * 因此导入时不应因为上游标记版权受限就顺带把作品下架。
     */
    public void updateContentPublication(boolean published, String rightsStatus, String licenseNote,
            String fullContentObjectKey, String coverObjectKey) {
        this.published = published;
        this.rightsStatus = rightsStatus == null || rightsStatus.isBlank() ? "UNKNOWN" : rightsStatus;
        this.licenseNote = licenseNote == null ? "" : licenseNote;
        this.fullContentObjectKey = fullContentObjectKey;
        this.coverObjectKey = coverObjectKey;
    }
    /**
     * 详情补抓专用：只写简介与补全标记，其余字段一概不碰。
     *
     * 不复用 refreshFromWenku8 的原因有三：详情响应比列表页更旧，全量回写会把同步刚拿到的
     * title/author/tags 回退；它会无条件重写 sourceSyncedAt，凭空声明「刚做过目录同步」；
     * 且补抓路径上实体是跨网络调用持有的，写入面越窄越安全。
     *
     * 上游返回空简介时**仍然写入 detailSyncedAt**——否则每次打开都会重抓一本没有简介的书，
     * 持续消耗与正文导入共享的 5 秒节流额度。
     */
    public void applyDetailDescription(String detailDescription, Instant syncedAt) {
        if (detailDescription != null && !detailDescription.isBlank()) this.description = detailDescription;
        this.detailSyncedAt = syncedAt;
    }

    public void updateFullContentObject(String key, String sha256, long byteSize) {
        fullContentObjectKey = key; fullContentSha256 = sha256; fullContentByteSize = byteSize;
    }
    public void updateCoverObject(String key, String sha256, long byteSize) {
        coverObjectKey = key; coverSha256 = sha256; coverByteSize = byteSize;
    }
}
