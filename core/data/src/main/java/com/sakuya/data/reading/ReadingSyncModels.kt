package com.sakuya.data.reading

/** 阅读页展示和后台协调器共用的同步状态。 */
enum class ReadingSyncStatus { SYNCED, PENDING, SYNCING, FAILED }

data class OnlineReadingPosition(
    val bookId: String,
    val contentType: String,
    val progress: Float,
    val chapterId: String?,
    val chapterIndex: Int,
    val chapterProgress: Float,
    val modifiedAt: Long,
)

data class OnlineReadingBookmark(
    val id: String,
    val bookId: String,
    val title: String,
    val progress: Float,
    val note: String,
    val createdAt: Long,
    val chapterId: String?,
    val chapterProgress: Float,
)
