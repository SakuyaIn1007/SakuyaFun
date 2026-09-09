package com.sakuya.backend.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * Follow.java
 * 职责说明：保存单向关注关系，followerId 关注 followingId。
 * 执行流程：关注接口创建记录；列表和统计接口按两个用户 ID 查询该表；取消关注删除记录。
 * 说明：本实体独立于 friendships，避免“好友”与“关注”两个业务概念相互影响。
 */
@Entity
@Table(
    name = "follows",
    uniqueConstraints = @UniqueConstraint(name = "uk_follow_pair", columnNames = {"follower_id", "following_id"}),
    indexes = {
        @Index(name = "idx_follow_follower_created", columnList = "follower_id,created_at"),
        @Index(name = "idx_follow_following_created", columnList = "following_id,created_at")
    }
)
public class Follow {
    @Id private UUID id;
    /** 发起关注的用户 ID。 */
    @Column(name = "follower_id", nullable = false) private UUID followerId;
    /** 被关注的用户 ID。 */
    @Column(name = "following_id", nullable = false) private UUID followingId;
    /** 关注建立时间，用于列表稳定排序。 */
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected Follow() {}

    public Follow(UUID followerId, UUID followingId) {
        this.id = UUID.randomUUID();
        this.followerId = followerId;
        this.followingId = followingId;
        this.createdAt = Instant.now();
    }

    public UUID getFollowerId() { return followerId; }
    public UUID getFollowingId() { return followingId; }
    public Instant getCreatedAt() { return createdAt; }
}
