package com.sakuya.backend.chat;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="conversation_members",uniqueConstraints=@UniqueConstraint(columnNames={"conversation_id","user_id"}))
public class ConversationMember {
 @Id private UUID id; @Column(name="conversation_id",nullable=false) private UUID conversationId; @Column(name="user_id",nullable=false) private UUID userId; private boolean pinned; private long lastReadAt;
 protected ConversationMember(){} public ConversationMember(UUID conversationId,UUID userId){id=UUID.randomUUID();this.conversationId=conversationId;this.userId=userId;}
 public UUID getConversationId(){return conversationId;} public UUID getUserId(){return userId;} public boolean isPinned(){return pinned;} public long getLastReadAt(){return lastReadAt;} public void markRead(){lastReadAt=System.currentTimeMillis();}
}
