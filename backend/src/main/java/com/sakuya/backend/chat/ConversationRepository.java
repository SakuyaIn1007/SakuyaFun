package com.sakuya.backend.chat;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ConversationRepository.java
 * 职责说明：持久化会话主体，并通过单聊唯一键读取并发创建后唯一胜出的会话。
 */
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByDirectKey(String directKey);
}
