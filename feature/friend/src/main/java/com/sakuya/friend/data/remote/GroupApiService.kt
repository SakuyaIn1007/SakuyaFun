package com.sakuya.friend.data.remote

import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * GroupApiService.kt
 * 职责说明：声明群聊领域的远端接口与 DTO，覆盖列表、成员、公告和入群申请。
 * 执行流程：GroupRepository 调用本接口 -> 校验 BaseResponse -> 映射成 core:model 的群聊模型。
 */
interface GroupApiService {
    @GET("groups/conversations")
    suspend fun getGroupConversations(): Response<BaseResponse<List<GroupConversationDto>>>

    @GET("groups/{groupId}/members")
    suspend fun getMembers(@Path("groupId") groupId: String): Response<BaseResponse<List<GroupMemberDto>>>

    @GET("groups/{groupId}/announcements")
    suspend fun getAnnouncements(@Path("groupId") groupId: String): Response<BaseResponse<List<GroupAnnouncementDto>>>

    @GET("groups/{groupId}/join-requests")
    suspend fun getJoinRequests(@Path("groupId") groupId: String): Response<BaseResponse<List<GroupJoinRequestDto>>>

    @POST("groups/{groupId}/join-requests")
    suspend fun requestToJoin(@Path("groupId") groupId: String, @Body request: GroupJoinRequestBody): Response<BaseResponse<GroupJoinRequestDto>>

    @POST("groups/{groupId}/join-requests/{requestId}/approve")
    suspend fun approveJoinRequest(@Path("groupId") groupId: String, @Path("requestId") requestId: String): Response<BaseResponse<GroupJoinRequestDto>>

    @POST("groups/{groupId}/join-requests/{requestId}/reject")
    suspend fun rejectJoinRequest(@Path("groupId") groupId: String, @Path("requestId") requestId: String): Response<BaseResponse<GroupJoinRequestDto>>
}

data class GroupDto(val id: String, val name: String, val avatarText: String, val memberCount: Int, val description: String = "", val ownerId: String = "")
data class GroupConversationDto(val group: GroupDto, val lastMessage: String, val lastMessageAt: String = "", val unreadCount: Int = 0, val isPinned: Boolean = false)
data class GroupMemberDto(val userId: String, val displayName: String, val avatarText: String, val role: String, val joinedAt: String = "")
data class GroupAnnouncementDto(val id: String, val groupId: String, val content: String, val publisherName: String, val publishedAt: String)
data class GroupJoinRequestDto(val id: String, val groupId: String, val applicantId: String, val applicantName: String, val message: String = "", val status: String, val createdAt: String = "")
data class GroupJoinRequestBody(val message: String = "")
