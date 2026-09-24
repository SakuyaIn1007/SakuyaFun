package com.sakuya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


//
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessage: String,
    val timeLabel: String,
    val unreadCount: Int = 0,
    val avatarText: String?,
    /** 单聊对方头像；v15 新增，升级前的缓存以空串补齐，由 AvatarImage 回落默认图。 */
    val avatarUrl: String = "",
    val isPinned: Boolean = false,
    /** 仅表示当前账号对此会话的免打扰偏好，服务端同步后可离线保留。 */
    val isMuted: Boolean = false,
    val lastActiveTime: Long,
)
