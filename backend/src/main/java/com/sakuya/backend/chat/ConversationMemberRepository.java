package com.sakuya.backend.chat;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ConversationMemberRepository extends JpaRepository<ConversationMember,UUID>{List<ConversationMember> findByUserId(UUID userId); List<ConversationMember> findByConversationId(UUID conversationId); Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId,UUID userId); boolean existsByConversationIdAndUserId(UUID conversationId,UUID userId); void deleteByConversationIdAndUserId(UUID conversationId, UUID userId);}
