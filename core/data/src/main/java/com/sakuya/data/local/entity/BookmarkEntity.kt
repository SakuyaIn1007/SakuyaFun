package com.sakuya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书签持久化实体。
 * 全局进度用于旧版本兼容，章节字段用于在连续阅读中优先恢复精确位置。
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val title: String,
    val progress: Float,
    val note: String,
    val createdAt: Long,
    val chapterId: String? = null,
    val chapterProgress: Float = 0f,
)
