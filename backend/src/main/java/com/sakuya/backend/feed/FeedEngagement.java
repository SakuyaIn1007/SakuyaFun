package com.sakuya.backend.feed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * FeedEngagement.java
 * 职责说明：记录用户对动态的点赞或收藏。
 * 执行流程：互动接口按 enabled 创建或删除记录；唯一约束保证重复操作不会产生重复数据。
 */
@Entity
@Table(
    name = "feed_engagements",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_feed_engagement",
        columnNames = {"post_id", "user_id", "type"}
    )
)
public class FeedEngagement {
    public enum Type { LIKE, FAVORITE }

    @Id
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Type type;

    protected FeedEngagement() {}

    public FeedEngagement(UUID postId, UUID userId, Type type) {
        this.id = UUID.randomUUID();
        this.postId = postId;
        this.userId = userId;
        this.type = type;
    }

    public UUID getId() { return id; }
}
