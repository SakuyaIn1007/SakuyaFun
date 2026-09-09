package com.sakuya.backend.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * ChatMessage.java
 * 职责说明：保存消息正文、类型及回复快照；附件通过 ChatAttachment 的 messageId 关联。
 * 回复同时保留原消息 ID 和展示快照，因此原消息后续删除也不会让回复气泡失去上下文。
 */
@Entity @Table(name="chat_messages",indexes=@Index(name="idx_message_conversation_time",columnList="conversation_id,created_at"),uniqueConstraints=@UniqueConstraint(name="uk_chat_message_sender_client",columnNames={"sender_id","client_message_id"}))
public class ChatMessage {
 @Id private UUID id; @Column(name="conversation_id",nullable=false) private UUID conversationId; @Column(name="sender_id",nullable=false) private UUID senderId; @Column(nullable=false,length=4000) private String content; @Column(nullable=false,length=20) private String messageType; @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="reply_to_message_id") private UUID replyToMessageId;
 @Column(name="reply_sender_name",length=100) private String replySenderName;
 @Column(name="reply_preview",length=200) private String replyPreview;
 @Column(name="client_message_id",length=100) private String clientMessageId;
 protected ChatMessage(){}
 public ChatMessage(UUID conversationId,UUID senderId,String content,String messageType){this(conversationId,senderId,content,messageType,null,null,null,null);}
 public ChatMessage(UUID conversationId, UUID senderId, String content, String messageType, UUID replyToMessageId, String replySenderName, String replyPreview, String clientMessageId){id=UUID.randomUUID();this.conversationId=conversationId;this.senderId=senderId;this.content=content;this.messageType=messageType;this.replyToMessageId=replyToMessageId;this.replySenderName=replySenderName;this.replyPreview=replyPreview;this.clientMessageId=clientMessageId;createdAt=Instant.now();}
 public UUID getId(){return id;} public UUID getConversationId(){return conversationId;} public UUID getSenderId(){return senderId;} public String getContent(){return content;} public String getMessageType(){return messageType;} public Instant getCreatedAt(){return createdAt;}
 public UUID getReplyToMessageId(){return replyToMessageId;} public String getReplySenderName(){return replySenderName;} public String getReplyPreview(){return replyPreview;}
 public String getClientMessageId(){return clientMessageId;}
}
