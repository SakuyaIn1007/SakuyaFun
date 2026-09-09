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
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part

interface ConversationApiService {

    @GET("conversations")
    suspend fun getConversations(): Response<BaseResponse<List<ConversationDto>>>

    @GET("conversations/{id}")
    suspend fun getConversationDetail(@Path("id") conversationId: String): Response<BaseResponse<ConversationDetailDto>>

    @GET("conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") conversationId: String,
        @Query("before") beforeTimestamp: Long? = null,
        @Query("limit") limit: Int = 30
    ): Response<BaseResponse<List<ChatMessageDto>>>

    @GET("conversations/{id}/messages/search")
    suspend fun searchMessages(@Path("id") conversationId: String, @Query("keyword") keyword: String, @Query("limit") limit: Int = 30): Response<BaseResponse<List<ChatMessageDto>>>

    @GET("conversations/{id}/messages/context")
    suspend fun getMessageContext(@Path("id") conversationId: String, @Query("messageId") messageId: String, @Query("around") around: Int = 20): Response<BaseResponse<List<ChatMessageDto>>>

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") conversationId: String,
        @Body request: SendMessageRequest
    ): Response<BaseResponse<ChatMessageDto>>

    /** 媒体先独立上传，成功取得服务端附件 ID 后才能随消息发送。 */
    @Multipart
    @POST("conversations/{id}/attachments")
    suspend fun uploadAttachment(
        @Path("id") conversationId: String,
        @Part file: MultipartBody.Part,
    ): Response<BaseResponse<ChatAttachmentDto>>

    @DELETE("conversations/{id}/attachments/{attachmentId}")
    suspend fun discardAttachment(
        @Path("id") conversationId: String,
        @Path("attachmentId") attachmentId: String,
    ): Response<BaseResponse<Unit>>

    @PUT("conversations/{id}/read")
    suspend fun markAsRead(
        @Path("id") conversationId: String
    ): Response<BaseResponse<Unit>>

    @PATCH("conversations/{id}/preferences")
    suspend fun updatePreferences(@Path("id") conversationId: String, @Body request: UpdateConversationPreferencesRequest): Response<BaseResponse<ConversationDetailDto>>

    @DELETE("conversations/{id}/membership")
    suspend fun leaveGroup(@Path("id") conversationId: String): Response<BaseResponse<Unit>>
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

data class ConversationDetailDto(
    val id: String,
    val type: String,
    val title: String,
    val avatarText: String,
    val description: String = "",
    val announcement: String = "",
    val directProfile: DirectProfileDto? = null,
    val members: List<ConversationMemberDto> = emptyList(),
    val preferences: ConversationPreferencesDto,
)

data class DirectProfileDto(val userId: String, val nickname: String, val avatarUrl: String = "", val signature: String? = null, val isOnline: Boolean = false)
data class ConversationMemberDto(val userId: String, val displayName: String, val avatarText: String)
data class ConversationPreferencesDto(val pinned: Boolean, val muted: Boolean, val memberNickname: String = "")
data class UpdateConversationPreferencesRequest(val pinned: Boolean? = null, val muted: Boolean? = null, val memberNickname: String? = null)

data class SendMessageRequest(
    val content: String,
    val messageType: String = "text",
    val attachmentIds: List<String> = emptyList(),
    val replyToMessageId: String? = null,
    val clientMessageId: String? = null,
){
    companion object {
        fun text(content: String) = SendMessageRequest(content, MessageType.TEXT.typeName)
        fun image(url: String) = SendMessageRequest(url, MessageType.IMAGE.typeName)
        fun file(url: String) = SendMessageRequest(url, MessageType.FILE.typeName)
    }
}
