package com.sakuya.conversation.data.remote

data class ChatMessageDto(
    val id: String,
    val conversationId: String,
    val content: String,
    val timeLabel: String,
    val isMine: Boolean,
    val avatarText: String = "",
    val timeStamp: Long = 0,
    val messageType: String = "text",
    val senderId: String = "",
    val attachments: List<ChatAttachmentDto> = emptyList(),
    val replyTo: ChatMessageReplyDto? = null,
    val readStatus: String = "unread",
)

/** 仅服务于网络传输；Repository 负责转换为 ChatAttachment 领域模型。 */
data class ChatAttachmentDto(
    val id: String,
    val type: String,
    val url: String,
    val name: String = "",
    val sizeBytes: Long? = null,
    val thumbnailUrl: String? = null,
)

data class ChatMessageReplyDto(val messageId: String, val senderName: String, val preview: String)
