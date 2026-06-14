package com.sakuya.conversation.data.repository

import com.sakuya.conversation.data.remote.ChatMessageDto
import com.sakuya.conversation.data.remote.ChatWebSocket
import com.sakuya.conversation.data.remote.ConversationApiService
import com.sakuya.data.local.entity.ChatMessageEntity
import com.sakuya.data.local.entity.MessageType
import com.sakuya.model.network.BaseResponse
import kotlinx.coroutines.flow.SharedFlow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ConversationApiService,
    private val webSocket: ChatWebSocket
) {
    val realtimeMessages: SharedFlow<ChatMessageDto> = webSocket.messages

    suspend fun getHistoryMessages(conversationId: String): Result<List<ChatMessageDto>> {
        return apiService.getMessages(conversationId).toResult()
    }

    fun sendMessage(conversationId: String, content: String) {
        webSocket.sendMessage(conversationId, content)
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

fun ChatMessageDto.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = this.id,
    conversationId = this.conversationId,
    content = this.content,
    timeLabel = this.timeLabel,
    isMine = this.isMine,
    timeStamp = this.timeStamp,
    messageType = MessageType.fromString(this.messageType)
)

fun ChatMessageEntity.toDto(avatarText: String = ""): ChatMessageDto = ChatMessageDto(
    id = this.id,
    conversationId = this.conversationId,
    content = this.content,
    timeLabel = this.timeLabel,
    isMine = this.isMine,
    avatarText = avatarText,
    timeStamp = this.timeStamp,
    messageType = this.messageType.typeName
)
