package com.sakuya.backend.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

/** 聊天附件按消息稳定排序读取；下载时同时校验附件所属会话。 */
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, UUID> {
    List<ChatAttachment> findByMessageIdOrderByCreatedAtAsc(UUID messageId);
    Optional<ChatAttachment> findByIdAndConversationId(UUID id, UUID conversationId);
    List<ChatAttachment> findByMessageIdIsNullAndCreatedAtBefore(Instant cutoff);
}
