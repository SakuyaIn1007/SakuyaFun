package com.sakuya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 服务端通知的本地快照；账号退出时与其他账号缓存一起清理。 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val actorId: String,
    val type: String,
    val title: String,
    val content: String,
    val targetType: String,
    val targetId: String,
    val targetUserId: String?,
    val createdAt: String,
    val readAt: String?,
)
