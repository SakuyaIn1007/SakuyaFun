package com.sakuya.backend.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * AppNotification.java
 * 职责说明：保存用户可追溯的一条业务通知，是通知中心与未读数的唯一真相源。
 * 执行流程：业务事务创建通知 -> Outbox 异步派发 FCM -> 客户端通过 REST 同步并标记已读。
 */
@Entity
@Table(name = "app_notifications", uniqueConstraints = @UniqueConstraint(name = "uk_notification_source", columnNames = "source_key"),
    indexes = {@Index(name = "idx_notification_recipient_created", columnList = "recipient_id,created_at"), @Index(name = "idx_notification_recipient_read", columnList = "recipient_id,read_at")})
public class AppNotification {
    public enum Type { FEED_COMMENT, FEED_REPLY, FEED_LIKE, FEED_FAVORITE, NEW_FOLLOWER, FRIEND_REQUEST, FRIEND_ACCEPTED, CHAPTER_UPDATE }
    public enum TargetType { FEED_POST, USER, FRIEND_REQUEST, BOOK_CHAPTER }

    @Id private UUID id;
    @Column(name = "recipient_id", nullable = false) private UUID recipientId;
    @Column(name = "actor_id", nullable = false) private UUID actorId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private Type type;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 500) private String content;
    @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 24) private TargetType targetType;
    @Column(name = "target_id", nullable = false) private String targetId;
    @Column(name = "target_user_id") private UUID targetUserId;
    @Column(name = "aggregate_key", length = 160) private String aggregateKey;
    @Column(name = "source_key", nullable = false, length = 220) private String sourceKey;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "read_at") private Instant readAt;

    protected AppNotification() {}
    public AppNotification(UUID recipientId, UUID actorId, Type type, String title, String content, TargetType targetType,
                           String targetId, UUID targetUserId, String aggregateKey, String sourceKey) {
        this.id = UUID.randomUUID(); this.recipientId = recipientId; this.actorId = actorId; this.type = type;
        this.title = title; this.content = content; this.targetType = targetType; this.targetId = targetId;
        this.targetUserId = targetUserId; this.aggregateKey = aggregateKey; this.sourceKey = sourceKey; this.createdAt = Instant.now();
    }
    public UUID getId(){return id;} public UUID getRecipientId(){return recipientId;} public UUID getActorId(){return actorId;}
    public Type getType(){return type;} public String getTitle(){return title;} public String getContent(){return content;}
    public TargetType getTargetType(){return targetType;} public String getTargetId(){return targetId;}
    public UUID getTargetUserId(){return targetUserId;} public String getAggregateKey(){return aggregateKey;}
    public Instant getCreatedAt(){return createdAt;} public Instant getReadAt(){return readAt;}
    public void markRead(){if(readAt==null)readAt=Instant.now();}
}
