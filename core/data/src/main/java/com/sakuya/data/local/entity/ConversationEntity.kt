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
    val isPinned: Boolean = false,
    val lastActiveTime: Long,
)
