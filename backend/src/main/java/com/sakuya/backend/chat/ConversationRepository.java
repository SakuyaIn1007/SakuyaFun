package com.sakuya.backend.chat;
import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ConversationRepository extends JpaRepository<Conversation,UUID>{}
