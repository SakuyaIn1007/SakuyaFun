package com.sakuya.conversation.model

import com.sakuya.data.local.entity.ConversationEntity


data class Conversation(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timeLabel: String,
    val unreadCount: Int = 0,
    val avatarText: String,
    val isPinned: Boolean = false
)

