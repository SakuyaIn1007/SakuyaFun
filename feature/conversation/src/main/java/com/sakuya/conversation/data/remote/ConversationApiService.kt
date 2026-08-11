package com.sakuya.conversation.data.remote

import com.sakuya.data.local.entity.MessageType
import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PUT

interface ConversationApiService {

    @GET("conversations")
    suspend fun getConversations(): Response<BaseResponse<List<ConversationDto>>>

    @GET("conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") conversationId: String,
        @Query("before") beforeTimestamp: Long? = null,
        @Query("limit") limit: Int = 30
    ): Response<BaseResponse<List<ChatMessageDto>>>

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") conversationId: String,
        @Body request: SendMessageRequest
    ): Response<BaseResponse<ChatMessageDto>>

    @PUT("conversations/{id}/read")
    suspend fun markAsRead(
        @Path("id") conversationId: String
    ): Response<BaseResponse<Unit>>
}

data class ConversationDto(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timeLabel: String,
    val unreadCount: Int = 0,
    val avatarText: String,
    val isPinned: Boolean = false
)

data class SendMessageRequest(
    val content: String,
    val messageType: String = "text",
    val attachments: List<ChatAttachmentDto> = emptyList(),
    val replyToMessageId: String? = null,
){
    companion object {
        fun text(content: String) = SendMessageRequest(content, MessageType.TEXT.typeName)
        fun image(url: String) = SendMessageRequest(url, MessageType.IMAGE.typeName)
        fun file(url: String) = SendMessageRequest(url, MessageType.FILE.typeName)
    }
}
