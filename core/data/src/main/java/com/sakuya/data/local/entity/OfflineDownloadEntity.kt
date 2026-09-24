package com.sakuya.data.local.entity

import androidx.room.Entity

/**
 * OfflineDownloadEntity.kt
 * 职责说明：按账号和在线书保存持久化下载任务，进程退出后仍可继续、暂停或重试。
 * 执行流程：用户入队 -> Coordinator 逐章下载并更新计数 -> 完成后 Reader 从离线章节表组装正文。
 */
@Entity(tableName = "offline_downloads", primaryKeys = ["ownerId", "bookId"])
data class OfflineDownloadEntity(
    val ownerId: String,
    val bookId: String,
    val title: String,
    val status: String,
    val completedChapters: Int = 0,
    val totalChapters: Int = 0,
    val downloadedBytes: Long = 0,
    val errorMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    /**
     * 最近一次被阅读的时间，是容量超限时 LRU 淘汰的依据。
     * 必须与 updatedAt 分开：updatedAt 记录下载进度，用它排序会优先淘汰
     * 「下载完就没再动过」的书，而那恰恰可能是用户最想保留的。
     */
    val lastAccessedAt: Long = System.currentTimeMillis(),
    /** 用户显式钉住后不参与自动淘汰。 */
    val pinned: Boolean = false,
)

/** 已落盘章节和排序元数据；文件内容不进入 Room，避免大正文撑大数据库。 */
@Entity(tableName = "offline_chapters", primaryKeys = ["ownerId", "bookId", "chapterId"])
data class OfflineChapterEntity(
    val ownerId: String,
    val bookId: String,
    val chapterId: String,
    val title: String,
    val volumeTitle: String,
    val chapterOrder: Int,
    val filePath: String,
    val byteSize: Long,
)
