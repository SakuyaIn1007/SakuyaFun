package com.sakuya.conversation.data.repository

import com.sakuya.conversation.data.remote.ChatMessageDto
import com.sakuya.conversation.data.remote.ChatWebSocket
import com.sakuya.conversation.data.remote.ConversationApiService
import com.sakuya.conversation.data.remote.SendMessageRequest
import com.google.gson.Gson
import com.sakuya.conversation.model.ChatAttachment
import com.sakuya.conversation.model.ChatAttachmentType
import com.sakuya.conversation.model.ChatMessage
import com.sakuya.conversation.model.ChatMessageDraft
import com.sakuya.conversation.model.ChatMessageReply
import com.sakuya.conversation.model.ChatMessageType
import com.sakuya.conversation.model.ChatReadStatus
import com.sakuya.conversation.model.ChatSendStatus
import com.sakuya.data.local.entity.ChatMessageEntity
import com.sakuya.data.local.entity.ConversationEntity
import com.sakuya.data.local.entity.MessageType
import com.sakuya.data.local.dao.ChatMessageDao
import com.sakuya.data.local.dao.ConversationDao
import com.sakuya.model.network.BaseResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ConversationApiService,
    private val webSocket: ChatWebSocket,
    private val chatMessageDao: ChatMessageDao,
    private val conversationDao: ConversationDao,
    private val gson: Gson,
) {
    /** WebSocket DTO 在仓库边界转换，保证 ViewModel 与 UI 永远不会依赖网络模型。 */
    val realtimeMessages = webSocket.messages.map(ChatMessageDto::toDomain)
    val connectionState = webSocket.connectionState

    /**
     * 聊天记录以 Room 为单一页面数据源。
     * 执行流程：ViewModel 先订阅本流显示缓存；历史接口和 WebSocket 只负责更新消息表。
     */
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        chatMessageDao.observeMessages(conversationId).map { messages -> messages.map { it.toDomain(gson) } }

    /**
     * 确保消息表的外键父记录存在。
     * 执行流程：从好友页直接打开聊天时先创建最小会话缓存，随后历史消息和实时消息才可安全写入 Room。
     */
    suspend fun ensureConversation(conversationId: String, title: String = "") {
        if (conversationDao.getById(conversationId) != null) return
        conversationDao.upsert(
            ConversationEntity(
                id = conversationId,
                title = title.ifBlank { "会话" },
                lastMessage = "",
                timeLabel = "",
                avatarText = null,
                lastActiveTime = System.currentTimeMillis(),
            )
        )
    }

    /** 后台拉取完整历史记录并按消息 ID 覆盖写入本地，不直接向 UI 返回网络数据。 */
    suspend fun syncHistoryMessages(conversationId: String): Result<Unit> {
        return try {
            ensureConversation(conversationId)
            apiService.getMessages(conversationId).toResult().map { messages ->
                chatMessageDao.upsertMessages(messages.map(ChatMessageDto::toDomain).map { it.toEntity(gson) })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(conversationId: String, draft: ChatMessageDraft): Result<ChatMessage> {
        return try {
            apiService.sendMessage(
                conversationId = conversationId,
                request = SendMessageRequest(
                    content = draft.content,
                    messageType = draft.attachments.firstOrNull()?.type?.name?.lowercase() ?: "text",
                    attachments = draft.attachments.map { attachment ->
                        com.sakuya.conversation.data.remote.ChatAttachmentDto(
                            id = attachment.id, type = attachment.type.name.lowercase(), url = attachment.url,
                            name = attachment.name, sizeBytes = attachment.sizeBytes, thumbnailUrl = attachment.thumbnailUrl,
                        )
                    },
                    replyToMessageId = draft.replyTo?.messageId,
                )
            ).toResult().map(ChatMessageDto::toDomain).onSuccess { message ->
                chatMessageDao.upsertMessage(message.toEntity(gson))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 本地乐观消息先落库，确保离开页面或重建 ViewModel 后仍能看到发送状态。 */
    suspend fun saveLocalMessage(message: ChatMessage) {
        ensureConversation(message.conversationId)
        chatMessageDao.upsertMessage(message.toEntity(gson))
    }

    /** 服务端确认后删除临时 ID 的 pending 记录，正式记录由 sendMessage 写入。 */
    suspend fun removeLocalMessage(messageId: String) {
        chatMessageDao.deleteMessageById(messageId)
    }

    /** WebSocket 收到消息时写入消息表和会话摘要，页面通过 observeMessages 自动更新。 */
    suspend fun saveRealtimeMessage(message: ChatMessage) {
        ensureConversation(message.conversationId)
        chatMessageDao.handleIncomingMessage(message.toEntity(gson))
    }

    suspend fun markAsRead(conversationId: String): Result<Unit> {
        return try {
            val response = apiService.markAsRead(conversationId)
            val body = response.body()
            if (response.isSuccessful && body != null && body.isSuccess()) {
                chatMessageDao.markConversationAsRead(conversationId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(body?.message ?: "标记已读失败"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun connect() = webSocket.connect()
    fun disconnect() = webSocket.disconnect()
}

private fun <T> Response<BaseResponse<T>>.toResult(): Result<T> {
    val body = body()
    return if (isSuccessful && body != null) {
        body.toResult()
    } else {
        Result.failure(Exception("HTTP ${code()}"))
    }
}

fun ChatMessage.toEntity(gson: Gson): ChatMessageEntity = ChatMessageEntity(
    id = this.id,
    conversationId = this.conversationId,
    senderId = this.senderId,
    content = this.content,
    timeLabel = this.timeLabel,
    isMine = this.isMine,
    avatarText = this.avatarText,
    timeStamp = this.timestamp,
    messageType = MessageType.fromString(this.type.name.lowercase()),
    attachmentsJson = gson.toJson(this.attachments),
    replyJson = this.replyTo?.let(gson::toJson),
    sendStatus = this.sendStatus.name.lowercase(),
    readStatus = this.readStatus.name.lowercase(),
)

fun ChatMessageEntity.toDomain(gson: Gson): ChatMessage = ChatMessage(
    id = this.id,
    conversationId = this.conversationId,
    senderId = this.senderId,
    content = this.content,
    timeLabel = this.timeLabel,
    isMine = this.isMine,
    avatarText = this.avatarText,
    timestamp = this.timeStamp,
    type = runCatching { ChatMessageType.valueOf(this.messageType.typeName.uppercase()) }.getOrDefault(ChatMessageType.TEXT),
    attachments = runCatching {
        gson.fromJson(this.attachmentsJson, Array<ChatAttachment>::class.java).toList()
    }.getOrDefault(emptyList()),
    replyTo = this.replyJson?.let { replyJson ->
        runCatching { gson.fromJson(replyJson, ChatMessageReply::class.java) }.getOrNull()
    },
    sendStatus = runCatching { ChatSendStatus.valueOf(this.sendStatus.uppercase()) }.getOrDefault(ChatSendStatus.SENT),
    readStatus = runCatching { ChatReadStatus.valueOf(this.readStatus.uppercase()) }.getOrDefault(ChatReadStatus.UNREAD),
)

/** DTO 到领域模型的唯一映射入口，防止网络字段继续渗入 UI。 */
fun ChatMessageDto.toDomain(): ChatMessage = ChatMessage(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    content = content,
    timeLabel = timeLabel,
    isMine = isMine,
    avatarText = avatarText,
    timestamp = timeStamp,
    type = runCatching { ChatMessageType.valueOf(messageType.uppercase()) }.getOrDefault(ChatMessageType.TEXT),
    attachments = attachments.map { attachment ->
        ChatAttachment(
            id = attachment.id,
            type = runCatching { ChatAttachmentType.valueOf(attachment.type.uppercase()) }.getOrDefault(ChatAttachmentType.FILE),
            url = attachment.url,
            name = attachment.name,
            sizeBytes = attachment.sizeBytes,
            thumbnailUrl = attachment.thumbnailUrl,
        )
    },
    replyTo = replyTo?.let { ChatMessageReply(it.messageId, it.senderName, it.preview) },
    readStatus = if (readStatus.equals("read", ignoreCase = true)) ChatReadStatus.READ else ChatReadStatus.UNREAD,
)
