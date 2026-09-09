package com.sakuya.conversation.mapper

import com.sakuya.model.chat.Conversation
import com.sakuya.data.local.entity.ConversationEntity

fun ConversationEntity.toDomainModel(): Conversation {
    return Conversation(
        id = this.id,
        title = this.title,
        lastMessage = this.lastMessage,
        timeLabel = this.timeLabel,
        unreadCount = this.unreadCount,
        avatarText = this.avatarText ?: "S",
        isPinned = this.isPinned
    )
}