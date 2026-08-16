package com.sakuya.friend.data.repository

import com.sakuya.friend.data.remote.GroupAnnouncementDto
import com.sakuya.friend.data.remote.GroupApiService
import com.sakuya.friend.data.remote.GroupConversationDto
import com.sakuya.friend.data.remote.GroupDto
import com.sakuya.friend.data.remote.GroupJoinRequestBody
import com.sakuya.friend.data.remote.GroupJoinRequestDto
import com.sakuya.friend.data.remote.GroupMemberDto
import com.sakuya.model.group.Group
import com.sakuya.model.group.GroupAnnouncement
import com.sakuya.model.group.GroupConversation
import com.sakuya.model.group.GroupJoinRequest
import com.sakuya.model.group.GroupJoinRequestStatus
import com.sakuya.model.group.GroupMember
import com.sakuya.model.group.GroupMemberRole
import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GroupRepository.kt
 * 职责说明：封装群聊 API 调用、错误处理与 DTO 到领域模型的映射。
 * 执行流程：GroupViewModel 只调用此类；网络层变化或本地缓存接入均不会影响群聊 UI。
 */
@Singleton
class GroupRepository @Inject constructor(private val apiService: GroupApiService) {
    suspend fun getGroupConversations(): Result<List<GroupConversation>> =
        apiService.getGroupConversations().toDomain { it.map(GroupConversationDto::toDomain) }

    suspend fun getMembers(groupId: String): Result<List<GroupMember>> =
        apiService.getMembers(groupId).toDomain { it.map(GroupMemberDto::toDomain) }

    suspend fun getAnnouncements(groupId: String): Result<List<GroupAnnouncement>> =
        apiService.getAnnouncements(groupId).toDomain { it.map(GroupAnnouncementDto::toDomain) }

    suspend fun getJoinRequests(groupId: String): Result<List<GroupJoinRequest>> =
        apiService.getJoinRequests(groupId).toDomain { it.map(GroupJoinRequestDto::toDomain) }

    suspend fun requestToJoin(groupId: String, message: String): Result<GroupJoinRequest> =
        apiService.requestToJoin(groupId, GroupJoinRequestBody(message)).toDomain(GroupJoinRequestDto::toDomain)

    suspend fun approveJoinRequest(groupId: String, requestId: String): Result<GroupJoinRequest> =
        apiService.approveJoinRequest(groupId, requestId).toDomain(GroupJoinRequestDto::toDomain)

    suspend fun rejectJoinRequest(groupId: String, requestId: String): Result<GroupJoinRequest> =
        apiService.rejectJoinRequest(groupId, requestId).toDomain(GroupJoinRequestDto::toDomain)
}

private fun GroupConversationDto.toDomain() = GroupConversation(group.toDomain(), lastMessage, lastMessageAt, unreadCount, isPinned)
private fun GroupDto.toDomain() = Group(id, name, avatarText, memberCount, description, ownerId)
private fun GroupMemberDto.toDomain() = GroupMember(userId, displayName, avatarText, runCatching { GroupMemberRole.valueOf(role.uppercase()) }.getOrDefault(GroupMemberRole.MEMBER), joinedAt)
private fun GroupAnnouncementDto.toDomain() = GroupAnnouncement(id, groupId, content, publisherName, publishedAt)
private fun GroupJoinRequestDto.toDomain() = GroupJoinRequest(id, groupId, applicantId, applicantName, message, runCatching { GroupJoinRequestStatus.valueOf(status.uppercase()) }.getOrDefault(GroupJoinRequestStatus.PENDING), createdAt)

private fun <T, R> Response<BaseResponse<T>>.toDomain(mapper: (T) -> R): Result<R> =
    if (!isSuccessful) Result.failure(IllegalStateException("HTTP ${code()}"))
    else body()?.toResult()?.map(mapper) ?: Result.failure(IllegalStateException("服务器未返回数据"))
