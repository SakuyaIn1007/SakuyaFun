package com.sakuya.conversation.data.remote

data class ChatMessageDto(
    val id: String,
    val conversationId: String,
    val content: String,
    val timeLabel: String,
    val isMine: Boolean,
    val avatarText: String = "",
    val timeStamp: Long = 0,
    val messageType: String = "text"
)
