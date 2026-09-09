package com.sakuya.backend.chat;
import java.time.Instant; import java.util.*; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.JpaRepository;
public interface ChatMessageRepository extends JpaRepository<ChatMessage,UUID>{
 List<ChatMessage> findByConversationIdOrderByCreatedAtDesc(UUID conversationId,Pageable pageable);
 List<ChatMessage> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID conversationId,Instant before,Pageable pageable);
 List<ChatMessage> findByConversationIdAndCreatedAtLessThanEqualOrderByCreatedAtDesc(UUID conversationId, Instant at, Pageable pageable);
 List<ChatMessage> findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID conversationId, Instant at, Pageable pageable);
 List<ChatMessage> findByConversationIdAndMessageTypeAndContentContainingIgnoreCaseOrderByCreatedAtDesc(UUID conversationId, String messageType, String keyword, Pageable pageable);
 Optional<ChatMessage> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);
 Optional<ChatMessage> findBySenderIdAndClientMessageId(UUID senderId,String clientMessageId);
 long countByConversationIdAndCreatedAtAfterAndSenderIdNot(UUID conversationId,Instant after,UUID senderId);
}
