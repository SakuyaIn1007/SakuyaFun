package com.sakuya.backend.chat;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="chat_messages",indexes=@Index(name="idx_message_conversation_time",columnList="conversation_id,created_at"))
public class ChatMessage {
 @Id private UUID id; @Column(name="conversation_id",nullable=false) private UUID conversationId; @Column(name="sender_id",nullable=false) private UUID senderId; @Column(nullable=false,length=4000) private String content; @Column(nullable=false,length=20) private String messageType; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected ChatMessage(){} public ChatMessage(UUID conversationId,UUID senderId,String content,String messageType){id=UUID.randomUUID();this.conversationId=conversationId;this.senderId=senderId;this.content=content;this.messageType=messageType;createdAt=Instant.now();}
 public UUID getId(){return id;} public UUID getConversationId(){return conversationId;} public UUID getSenderId(){return senderId;} public String getContent(){return content;} public String getMessageType(){return messageType;} public Instant getCreatedAt(){return createdAt;}
}
