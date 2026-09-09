package com.sakuya.backend.chat;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * ConversationMember.java
 * 职责说明：保存用户在某一会话中的阅读位置和个人偏好，避免将置顶、免打扰等设置误写为全局属性。
 * 执行流程：详情页更新请求只修改当前登录用户对应的成员记录；群名片同样仅在该用户的群成员展示中生效。
 */
@Entity
@Table(name = "conversation_members", uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
public class ConversationMember {
    @Id private UUID id;
    @Column(name = "conversation_id", nullable = false) private UUID conversationId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    private boolean pinned;
    private boolean muted;
    @Column(length = 40) private String memberNickname = "";
    private long lastReadAt;

    protected ConversationMember() {}
    public ConversationMember(UUID conversationId, UUID userId) {
        id = UUID.randomUUID(); this.conversationId = conversationId; this.userId = userId;
    }
    public UUID getConversationId() { return conversationId; }
    public UUID getUserId() { return userId; }
    public boolean isPinned() { return pinned; }
    public boolean isMuted() { return muted; }
    public String getMemberNickname() { return memberNickname == null ? "" : memberNickname; }
    public long getLastReadAt() { return lastReadAt; }
    public void markRead() { lastReadAt = System.currentTimeMillis(); }
    public void updatePreferences(Boolean newPinned, Boolean newMuted, String newMemberNickname) {
        if (newPinned != null) pinned = newPinned;
        if (newMuted != null) muted = newMuted;
        if (newMemberNickname != null) memberNickname = newMemberNickname.trim();
    }
}
