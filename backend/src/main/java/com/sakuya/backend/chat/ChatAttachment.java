package com.sakuya.backend.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * ChatAttachment.java
 * 职责说明：保存聊天上传文件的归属、类型、大小与物理存储键，消息只引用已经完成上传的附件。
 * 执行流程：会话成员上传 -> 创建未绑定附件 -> ChatService 校验归属并绑定消息 -> 鉴权下载。
 */
@Entity
@Table(
    name = "chat_attachments",
    indexes = {
        @Index(name = "idx_chat_attachment_conversation", columnList = "conversation_id"),
        @Index(name = "idx_chat_attachment_message", columnList = "message_id")
    }
)
public class ChatAttachment {
    @Id private UUID id;
    @Column(name = "conversation_id", nullable = false) private UUID conversationId;
    @Column(name = "uploader_id", nullable = false) private UUID uploaderId;
    @Column(name = "message_id") private UUID messageId;
    @Column(nullable = false, length = 16) private String type;
    @Column(name = "content_type", nullable = false, length = 100) private String contentType;
    @Column(name = "original_name", nullable = false, length = 255) private String originalName;
    @Column(name = "storage_name", nullable = false, unique = true, length = 100) private String storageName;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ChatAttachment() {}

    public ChatAttachment(UUID conversationId, UUID uploaderId, String type, String contentType, String originalName, String storageName, long sizeBytes) {
        this.id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.uploaderId = uploaderId;
        this.type = type;
        this.contentType = contentType;
        this.originalName = originalName;
        this.storageName = storageName;
        this.sizeBytes = sizeBytes;
        this.createdAt = Instant.now();
    }

    /** 附件只能绑定一次，防止同一上传对象被复制到多条消息。 */
    public void bindToMessage(UUID value) {
        if (messageId != null && !messageId.equals(value)) throw new IllegalStateException("附件已经绑定消息");
        messageId = value;
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public UUID getUploaderId() { return uploaderId; }
    public UUID getMessageId() { return messageId; }
    public String getType() { return type; }
    public String getContentType() { return contentType; }
    public String getOriginalName() { return originalName; }
    public String getStorageName() { return storageName; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getCreatedAt() { return createdAt; }
}
