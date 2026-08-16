package com.sakuya.model.group

/**
 * GroupModels.kt
 * 职责说明：定义群聊、成员、群会话、公告与入群申请的领域模型，供好友和会话模块复用。
 * 执行流程：GroupRepository 将远端 DTO 映射为这些模型，GroupViewModel 输出列表和操作状态给 UI。
 */
data class Group(
    val id: String,
    val name: String,
    val avatarText: String,
    val memberCount: Int,
    val description: String = "",
    val ownerId: String = "",
)

data class GroupMember(
    val userId: String,
    val displayName: String,
    val avatarText: String,
    val role: GroupMemberRole = GroupMemberRole.MEMBER,
    val joinedAt: String = "",
)

enum class GroupMemberRole { OWNER, ADMIN, MEMBER }

/** 群会话专属摘要，避免将群聊伪装成好友或私聊会话。 */
data class GroupConversation(
    val group: Group,
    val lastMessage: String,
    val lastMessageAt: String = "",
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
)

data class GroupAnnouncement(
    val id: String,
    val groupId: String,
    val content: String,
    val publisherName: String,
    val publishedAt: String,
)

data class GroupJoinRequest(
    val id: String,
    val groupId: String,
    val applicantId: String,
    val applicantName: String,
    val message: String = "",
    val status: GroupJoinRequestStatus = GroupJoinRequestStatus.PENDING,
    val createdAt: String = "",
)

enum class GroupJoinRequestStatus { PENDING, APPROVED, REJECTED }
