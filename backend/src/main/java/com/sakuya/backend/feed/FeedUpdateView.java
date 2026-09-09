package com.sakuya.backend.feed;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * FeedUpdateView.java
 * 职责说明：持久化用户对关注流整体或某位作者动态的查看游标，作为跨设备小红点状态的真相源。
 * 执行流程：首次同步建立整体基线；进入关注流更新整体游标；进入作者主页只更新该作者游标。
 */
@Entity
@Table(name="feed_update_views",uniqueConstraints=@UniqueConstraint(name="uk_feed_update_view",columnNames={"user_id","scope","target_key"}),
    indexes=@Index(name="idx_feed_update_view_user",columnList="user_id,scope"))
public class FeedUpdateView {
    public enum Scope { FOLLOWING_FEED, FOLLOWING_AUTHOR }

    @Id private UUID id;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private Scope scope;
    @Column(name="target_user_id") private UUID targetUserId;
    @Column(name="target_key",nullable=false,length=40) private String targetKey;
    @Column(name="read_at",nullable=false) private Instant readAt;

    protected FeedUpdateView() {}
    public FeedUpdateView(UUID userId,Scope scope,UUID targetUserId,Instant readAt){
        this.id=UUID.randomUUID();this.userId=userId;this.scope=scope;this.targetUserId=targetUserId;
        this.targetKey=targetUserId==null?"*":targetUserId.toString();this.readAt=readAt;
    }
    public Instant getReadAt(){return readAt;}
    public void markRead(Instant value){if(value.isAfter(readAt))readAt=value;}
}
