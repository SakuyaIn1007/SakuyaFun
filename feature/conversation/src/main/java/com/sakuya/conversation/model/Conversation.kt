package com.sakuya.conversation.model

data class Conversation(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timeLabel: String,
    val unreadCount: Int = 0,
    val avatarText: String,
    val isPinned: Boolean = false
)
