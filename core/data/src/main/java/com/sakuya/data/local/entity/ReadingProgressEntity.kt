package com.sakuya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 阅读位置持久化实体。
 * progress 保留为旧版本与列表页兼容字段；章节字段为连续阅读提供更稳定的恢复锚点。
 */
@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val bookKey: String,
    val progress: Float,
    val updatedAt: Long,
    val contentType: String = "TXT",
    val chapterId: String? = null,
    val chapterIndex: Int = 0,
    val chapterProgress: Float = 0f,
)
