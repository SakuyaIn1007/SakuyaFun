package com.sakuya.backend.feed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * FeedPost.java
 * 职责说明：保存一条已发布动态，是动态流、详情及互动统计的主记录。
 * 执行流程：FeedController 发布或编辑时写入本实体；读取接口再聚合评论、点赞和收藏数据。
 */
@Entity
@Table(
    name = "feed_posts",
    indexes = @Index(name = "idx_feed_post_created", columnList = "created_at")
)
public class FeedPost {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    /** 使用竖线保存标签，接口层会转换为列表。 */
    @Lob
    private String tags = "";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected FeedPost() {}

    public FeedPost(UUID userId, String title, String content, String tags) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.tags = tags == null ? "" : tags;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getTags() { return tags; }
    public Instant getCreatedAt() { return createdAt; }

    /** 编辑动态正文与标签，不允许修改作者和创建时间。 */
    public void update(String title, String content, String tags) {
        this.title = title;
        this.content = content;
        this.tags = tags == null ? "" : tags;
    }
}
