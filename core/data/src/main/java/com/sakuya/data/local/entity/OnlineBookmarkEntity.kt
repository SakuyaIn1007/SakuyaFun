package com.sakuya.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * OnlineBookmarkEntity.kt
 * 职责说明：保存用户在线书签、章节锚点和同步墓碑，不与本地文件书签混用。
 * 执行流程：本地生成 UUID 并立即展示 -> 后台上传 -> 服务端冲突合并后返回账号快照。
 */
@Entity(
    tableName = "online_bookmarks",
    primaryKeys = ["ownerId", "id"],
    indices = [Index(value = ["ownerId", "bookId"])],
)
data class OnlineBookmarkEntity(
    val ownerId: String,
    val id: String,
    val bookId: String,
    val title: String,
    val progress: Float,
    val note: String,
    val createdAt: Long,
    val chapterId: String? = null,
    val chapterProgress: Float = 0f,
    val modifiedAt: Long,
    val deviceId: String,
    val deleted: Boolean = false,
    val syncState: String = "PENDING",
)
