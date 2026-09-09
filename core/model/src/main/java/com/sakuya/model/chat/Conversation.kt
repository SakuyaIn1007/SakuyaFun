package com.sakuya.model.chat

/**
 * Conversation.kt
 * 职责说明：定义会话列表、聊天详情页与历史搜索页使用的领域模型，隔离 Retrofit DTO。
 * 执行流程：Repository 将接口响应和 Room Entity 映射为本模型；ViewModel 以不可变 UiState 交给 Compose 渲染。
 * 归属说明：下沉至 core:model，供 conversation 及关联业务（friend 发起聊天）共享。
 */
data class Conversation(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timeLabel: String,
    val unreadCount: Int = 0,
    val avatarText: String,
    val isPinned: Boolean = false
)

data class ConversationDetail(
    val id: String,
    val type: ConversationType,
    val title: String,
    val avatarText: String,
    val description: String = "",
    val announcement: String = "",
    val directProfile: DirectProfile? = null,
    val members: List<ConversationMember> = emptyList(),
    val preferences: ConversationPreferences = ConversationPreferences(),
)

enum class ConversationType { DIRECT, GROUP }
data class DirectProfile(val userId: String, val nickname: String, val avatarUrl: String, val signature: String?, val isOnline: Boolean)
data class ConversationMember(val userId: String, val displayName: String, val avatarText: String)
data class ConversationPreferences(val isPinned: Boolean = false, val isMuted: Boolean = false, val memberNickname: String = "")
