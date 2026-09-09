package com.sakuya.model.chat

/**
 * ChatMessage.kt
 * 职责说明：定义聊天页面使用的领域消息，独立于 Retrofit DTO 与 Room Entity。
 * 执行流程：Repository 将历史和 WebSocket DTO 映射为本模型，ViewModel 追加乐观消息并按发送结果更新状态。
 * 归属说明：聊天域模型下沉至 core:model，供 conversation 及关联业务（friend、feed 私信）共享。
 */
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String = "",
    val content: String,
    val timeLabel: String,
    val isMine: Boolean,
    val avatarText: String = "",
    val timestamp: Long = 0,
    val type: ChatMessageType = ChatMessageType.TEXT,
    val attachments: List<ChatAttachment> = emptyList(),
    val replyTo: ChatMessageReply? = null,
    val sendStatus: ChatSendStatus = ChatSendStatus.SENT,
    val readStatus: ChatReadStatus = ChatReadStatus.UNREAD,
)

enum class ChatMessageType { TEXT, IMAGE, FILE, SYSTEM }
enum class ChatSendStatus { PENDING, SENT, FAILED }
enum class ChatReadStatus { UNREAD, READ }

data class ChatAttachment(
    val id: String,
    val type: ChatAttachmentType,
    val url: String,
    val name: String = "",
    val sizeBytes: Long? = null,
    val thumbnailUrl: String? = null,
)

enum class ChatAttachmentType { IMAGE, FILE }

data class ChatMessageReply(
    val messageId: String,
    val senderName: String,
    val preview: String,
)

/** UI 提交给 ViewModel 的消息草稿，后续可直接承载图片、文件和回复。 */
data class ChatMessageDraft(
    val content: String = "",
    val attachments: List<ChatAttachment> = emptyList(),
    val replyTo: ChatMessageReply? = null,
)
