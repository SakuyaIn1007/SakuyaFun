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
 * FeedComment.java
 * 职责说明：保存动态评论及可选的被回复评论 ID。
 * 执行流程：评论接口创建记录；详情接口按动态 ID 和创建时间读取。
 */
@Entity
@Table(
    name = "feed_comments",
    indexes = @Index(name = "idx_feed_comment_post_created", columnList = "post_id,created_at")
)
public class FeedComment {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID authorId;

    @Lob
    @Column(nullable = false)
    private String content;

    private UUID replyToCommentId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected FeedComment() {}

    public FeedComment(UUID postId, UUID authorId, String content, UUID replyToCommentId) {
        this.id = UUID.randomUUID();
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
        this.replyToCommentId = replyToCommentId;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPostId() { return postId; }
    public UUID getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public UUID getReplyToCommentId() { return replyToCommentId; }
    public Instant getCreatedAt() { return createdAt; }
}
