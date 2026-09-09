package com.sakuya.data.local.entity

import androidx.room.Entity

/**
 * OnlineReadingProgressEntity.kt
 * 职责说明：按账号保存在线书阅读位置，同时承担离线同步队列记录。
 * 执行流程：阅读器先写 PENDING -> 协调器上传 -> 服务端快照覆盖为 SYNCED；删除记录在确认前保留墓碑。
 */
@Entity(tableName = "online_reading_progress", primaryKeys = ["ownerId", "bookId"])
data class OnlineReadingProgressEntity(
    val ownerId: String,
    val bookId: String,
    val contentType: String,
    val progress: Float,
    val chapterId: String? = null,
    val chapterIndex: Int = 0,
    val chapterProgress: Float = 0f,
    val modifiedAt: Long,
    val deviceId: String,
    val deleted: Boolean = false,
    val syncState: String = "PENDING",
)
