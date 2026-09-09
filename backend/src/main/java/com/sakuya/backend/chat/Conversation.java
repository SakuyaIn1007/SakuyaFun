package com.sakuya.backend.chat;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Conversation.java
 * 职责说明：持久化单聊和群聊共用的基础资料。
 * 执行流程：创建时明确写入会话类型；单聊额外保存按双方用户 ID 排序生成的唯一键，
 * 从而让并发的“创建或复用”请求在数据库层只能落下一条会话；成员个人设置存放在 ConversationMember 中。
 */
@Entity
@Table(name = "conversations")
public class Conversation {
    public enum Type { DIRECT, GROUP }

    @Id private UUID id;
    @Column(nullable = false) private String title;
    /** 保持可空以兼容已有数据库；读取旧记录时统一视为单聊。 */
    @Enumerated(EnumType.STRING) @Column(length = 16) private Type type = Type.DIRECT;
    /** 仅单聊使用；可空是为了兼容升级前已有会话，唯一约束负责兜底并发创建。 */
    @Column(length = 73, unique = true) private String directKey;
    @Column(length = 500) private String description = "";
    @Column(length = 1000) private String announcement = "";
    @Column(nullable = false) private Instant createdAt;

    protected Conversation() {}
    public Conversation(String title, Type type) {
        id = UUID.randomUUID(); this.title = title; this.type = type; createdAt = Instant.now();
    }
    public Conversation(String title, String directKey) {
        this(title, Type.DIRECT); this.directKey = directKey;
    }
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public Type getType() { return type == null ? Type.DIRECT : type; }
    public String getDirectKey() { return directKey; }
    /** 首次访问旧单聊时补写规范化键，使后续请求直接走唯一索引。 */
    public void assignDirectKey(String value) { if (directKey == null || directKey.isBlank()) directKey = value; }
    public String getDescription() { return description == null ? "" : description; }
    public String getAnnouncement() { return announcement == null ? "" : announcement; }
}
