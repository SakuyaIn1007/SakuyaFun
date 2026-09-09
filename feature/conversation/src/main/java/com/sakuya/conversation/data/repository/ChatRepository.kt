package com.sakuya.conversation.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sakuya.conversation.data.remote.ChatMessageDto
import com.sakuya.conversation.data.remote.ChatWebSocket
import com.sakuya.conversation.data.remote.ConversationApiService
import com.sakuya.conversation.data.remote.SendMessageRequest
import com.sakuya.conversation.data.remote.ConversationDetailDto
import com.sakuya.conversation.data.remote.UpdateConversationPreferencesRequest
import com.google.gson.Gson
import com.sakuya.model.chat.ChatAttachment
import com.sakuya.model.chat.ChatAttachmentType
import com.sakuya.model.chat.ChatMessage
import com.sakuya.model.chat.ChatMessageDraft
import com.sakuya.model.chat.ChatMessageReply
import com.sakuya.model.chat.ChatMessageType
import com.sakuya.model.chat.ChatReadStatus
import com.sakuya.model.chat.ChatSendStatus
import com.sakuya.model.chat.ConversationDetail
import com.sakuya.model.chat.ConversationMember
import com.sakuya.model.chat.ConversationPreferences
import com.sakuya.model.chat.ConversationType
import com.sakuya.model.chat.DirectProfile
import com.sakuya.data.local.entity.ChatMessageEntity
import com.sakuya.data.local.entity.ConversationEntity
import com.sakuya.data.local.entity.MessageType
import com.sakuya.data.local.dao.ChatMessageDao
import com.sakuya.data.local.dao.ConversationDao
import com.sakuya.model.network.BaseResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatRepository.kt
 * 职责说明：
 * 1. 协调 REST 历史消息、WebSocket 实时消息与 Room 持久化。
 * 2. 向 ViewModel 仅提供以 Room 为单一数据源的消息流，隔离网络 DTO 和连接细节。
 * 3. 持久化本地乐观消息及其发送失败状态，保证离线和页面重建后的可恢复性。
 * 执行流程：历史和实时数据先转换并写入 Room；页面订阅 Room 自动刷新；
 * WebSocket 的失败和退避重连留在底层处理，不向 UI 暴露连接错误状态。
 */
@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ConversationApiService,
    private val webSocket: ChatWebSocket,
    private val chatMessageDao: ChatMessageDao,
    private val conversationDao: ConversationDao,
    private val gson: Gson,
    @ApplicationContext private val context: Context,
) {
    /** WebSocket DTO 在仓库边界转换，保证 ViewModel 与 UI 永远不会依赖网络模型。 */
    val realtimeMessages = webSocket.messages.map(ChatMessageDto::toDomain)

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

    /**
     * 后台拉取历史记录并按消息 ID 覆盖写入本地，不直接向 UI 返回网络数据。
     * 执行流程：确保会话父记录 -> 批量写入消息 -> 清除本地未读；任一步失败都返回给 ViewModel 展示重试反馈。
     */
    suspend fun syncHistoryMessages(conversationId: String): Result<Unit> {
        return try {
            apiService.getMessages(conversationId).toResult().map { messages ->
                writeHistoryMessagesToRoom(
                    conversationId = conversationId,
                    items = messages,
                    gson = gson,
                    ensureConversation = ::ensureConversation,
                    writeMessages = chatMessageDao::upsertMessages,
                    markRead = chatMessageDao::markConversationAsRead,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(conversationId: String, draft: ChatMessageDraft, clientMessageId: String): Result<ChatMessage> {
        return try {
            apiService.sendMessage(
                conversationId = conversationId,
                request = SendMessageRequest(
                    content = draft.content,
                    messageType = draft.attachments.firstOrNull()?.type?.name?.lowercase() ?: "text",
                    attachmentIds = draft.attachments.map(ChatAttachment::id),
                    replyToMessageId = draft.replyTo?.messageId,
                    clientMessageId = clientMessageId,
                )
            ).toResult().map(ChatMessageDto::toDomain).onSuccess { message ->
                // REST 确认消息与本地乐观消息走同一跨表事务：正式时间和摘要会覆盖临时状态，
                // 而 isMine 分支保证发送方自己的消息不会增加未读数。
                chatMessageDao.handleIncomingMessage(message.toEntity(gson))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 上传单个聊天附件并返回服务端元数据。
     * 执行流程：ContentResolver 读取名称/类型/大小 -> 客户端先执行同服务端一致的限制 -> Multipart 上传。
     */
    suspend fun uploadAttachment(conversationId: String, uri: Uri): Result<ChatAttachment> = withContext(Dispatchers.IO) {
        request {
            val resolver = context.contentResolver
            val metadata = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) null else {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    (if (nameIndex >= 0) cursor.getString(nameIndex) else null) to (if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null)
                }
            }
            val fileName = metadata?.first?.takeIf(String::isNotBlank) ?: "attachment"
            val mimeType = resolver.getType(uri)?.lowercase() ?: "application/octet-stream"
            val isImage = mimeType.startsWith("image/")
            val allowed = if (isImage) IMAGE_MIME_TYPES else FILE_MIME_TYPES
            if (mimeType !in allowed) return@request Result.failure(IllegalArgumentException(if (isImage) "仅支持 JPG、PNG、WebP 或 GIF 图片" else "仅支持 PDF、TXT、ZIP 或 EPUB 文件"))
            val limit = if (isImage) MAX_IMAGE_BYTES else MAX_FILE_BYTES
            if (metadata?.second != null && metadata.second!! > limit) return@request Result.failure(IllegalArgumentException(if (isImage) "图片不能超过 10MB" else "文件不能超过 20MB"))
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@request Result.failure(IllegalArgumentException("无法读取附件"))
            if (bytes.size > limit) return@request Result.failure(IllegalArgumentException(if (isImage) "图片不能超过 10MB" else "文件不能超过 20MB"))
            val part = MultipartBody.Part.createFormData("file", fileName, bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
            apiService.uploadAttachment(conversationId, part).toResult().map { it.toDomainAttachment() }
        }
    }

    suspend fun discardAttachment(conversationId: String, attachmentId: String): Result<Unit> = request {
        apiService.discardAttachment(conversationId, attachmentId).toResult()
    }

    /**
     * 本地乐观消息先落库，并同步更新会话摘要。
     * 执行流程：PENDING 与 FAILED 均先写消息表，再在同一事务更新 lastMessage、timeLabel
     * 和 lastActiveTime；本地消息的 isMine=true 会确保 unreadCount 保持不变。
     */
    suspend fun saveLocalMessage(message: ChatMessage) {
        ensureConversation(message.conversationId)
        chatMessageDao.handleIncomingMessage(message.toEntity(gson))
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

    /** 详情、搜索和上下文均经由仓库映射，UI 不依赖网络 DTO。 */
    suspend fun getConversationDetail(conversationId: String): Result<ConversationDetail> = request {
        apiService.getConversationDetail(conversationId).toResult().map(ConversationDetailDto::toDomain)
    }

    suspend fun updatePreferences(conversationId: String, pinned: Boolean? = null, muted: Boolean? = null, memberNickname: String? = null): Result<ConversationDetail> = request {
        apiService.updatePreferences(conversationId, UpdateConversationPreferencesRequest(pinned, muted, memberNickname)).toResult().map(ConversationDetailDto::toDomain).onSuccess { detail ->
            writeConversationPreferencesToRoom(
                conversationId = conversationId,
                detail = detail,
                updatePreferences = conversationDao::updatePreferences,
            )
        }
    }

    suspend fun searchMessages(conversationId: String, keyword: String): Result<List<ChatMessage>> = request {
        apiService.searchMessages(conversationId, keyword).toResult().map { items -> items.map(ChatMessageDto::toDomain) }
    }

    /** 搜索跳转前将目标附近记录写入 Room，使聊天页始终从本地单一数据源渲染。 */
    suspend fun syncMessageContext(conversationId: String, messageId: String): Result<Unit> = request {
        apiService.getMessageContext(conversationId, messageId).toResult().map { items ->
            writeMessageContextToRoom(
                conversationId = conversationId,
                items = items,
                gson = gson,
                ensureConversation = ::ensureConversation,
                writeMessages = chatMessageDao::upsertMessages,
            )
        }
    }

    suspend fun clearLocalMessages(conversationId: String) = chatMessageDao.deleteMessageByConversation(conversationId)
    suspend fun leaveGroup(conversationId: String): Result<Unit> = request {
        apiService.leaveGroup(conversationId).toResult().onSuccess {
            removeLeftConversationFromRoom(
                conversationId = conversationId,
                findConversation = conversationDao::getById,
                deleteConversation = conversationDao::delete,
            )
        }
    }

    private suspend fun <T> request(block: suspend () -> Result<T>): Result<T> = try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }

    fun connect() = webSocket.connect()
    fun disconnect() = webSocket.disconnect()

    private companion object {
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
        const val MAX_FILE_BYTES = 20 * 1024 * 1024
        val IMAGE_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
        val FILE_MIME_TYPES = setOf("application/pdf", "text/plain", "application/zip", "application/epub+zip")
    }
}

/**
 * 将回跳上下文写入 Room 的最小事务边界。
 * 执行流程：先保证外键父会话存在，再统一完成 DTO -> 领域模型 -> Room 实体转换并批量覆盖；
 * 使用可替换的写入函数是为了在 JVM 回归测试中精确验证写入顺序和内容。
 */
internal suspend fun writeMessageContextToRoom(
    conversationId: String,
    items: List<ChatMessageDto>,
    gson: Gson,
    ensureConversation: suspend (String, String) -> Unit,
    writeMessages: suspend (List<ChatMessageEntity>) -> Unit,
) {
    ensureConversation(conversationId, "")
    writeMessages(items.map(ChatMessageDto::toDomain).map { it.toEntity(gson) })
}

/**
 * 首次进入聊天时的 Room 写入边界。
 * 执行流程：先补齐外键父记录，再保存服务端历史，最后同步服务端 GET 已完成的已读语义；
 * 空历史同样会执行已读更新，但不会伪造任何消息。
 */
internal suspend fun writeHistoryMessagesToRoom(
    conversationId: String,
    items: List<ChatMessageDto>,
    gson: Gson,
    ensureConversation: suspend (String, String) -> Unit,
    writeMessages: suspend (List<ChatMessageEntity>) -> Unit,
    markRead: suspend (String) -> Unit,
) {
    ensureConversation(conversationId, "")
    writeMessages(items.map(ChatMessageDto::toDomain).map { it.toEntity(gson) })
    markRead(conversationId)
}

/** 服务端保存成员偏好成功后才更新 Room，确保列表离线状态与后端真值一致。 */
internal suspend fun writeConversationPreferencesToRoom(
    conversationId: String,
    detail: ConversationDetail,
    updatePreferences: suspend (String, Boolean, Boolean) -> Unit,
) {
    updatePreferences(conversationId, detail.preferences.isPinned, detail.preferences.isMuted)
}

/**
 * 退出群聊成功后的本地清理边界。
 * 执行流程：按会话 ID 读取 Room 父记录，存在时删除；真实数据库通过外键级联清理对应消息。
 */
internal suspend fun removeLeftConversationFromRoom(
    conversationId: String,
    findConversation: suspend (String) -> ConversationEntity?,
    deleteConversation: suspend (ConversationEntity) -> Unit,
) {
    findConversation(conversationId)?.let { deleteConversation(it) }
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
    attachments = attachments.map { it.toDomainAttachment() },
    replyTo = replyTo?.let { ChatMessageReply(it.messageId, it.senderName, it.preview) },
    readStatus = if (readStatus.equals("read", ignoreCase = true)) ChatReadStatus.READ else ChatReadStatus.UNREAD,
)

private fun com.sakuya.conversation.data.remote.ChatAttachmentDto.toDomainAttachment() = ChatAttachment(
    id = id,
    type = runCatching { ChatAttachmentType.valueOf(type.uppercase()) }.getOrDefault(ChatAttachmentType.FILE),
    url = resolveChatMediaUrl(url),
    name = name,
    sizeBytes = sizeBytes,
    thumbnailUrl = thumbnailUrl?.let(::resolveChatMediaUrl),
)

/** 服务端在 REST 与 WebSocket 中统一返回相对媒体路径，客户端在仓库边界补齐当前环境 Base URL。 */
internal fun resolveChatMediaUrl(value: String): String {
    if (value.startsWith("http://") || value.startsWith("https://")) return value
    return com.sakuya.data.BuildConfig.API_BASE_URL.trimEnd('/') + "/" + value.trimStart('/')
}

internal fun ConversationDetailDto.toDomain(): ConversationDetail = ConversationDetail(
    id = id,
    type = runCatching { ConversationType.valueOf(type.uppercase()) }.getOrDefault(ConversationType.DIRECT),
    title = title,
    avatarText = avatarText,
    description = description,
    announcement = announcement,
    directProfile = directProfile?.let { DirectProfile(it.userId, it.nickname, it.avatarUrl, it.signature, it.isOnline) },
    members = members.map { ConversationMember(it.userId, it.displayName, it.avatarText) },
    preferences = ConversationPreferences(preferences.pinned, preferences.muted, preferences.memberNickname),
)
