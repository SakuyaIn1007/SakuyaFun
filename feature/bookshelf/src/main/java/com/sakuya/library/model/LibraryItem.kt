package com.sakuya.library.model

/**
 * LibraryItem.kt
 * 职责说明：书架卡片的领域模型，汇聚本地书籍、阅读进度和云端同步元数据。
 * 执行流程：LibraryRepository 将 Room 书籍与 ReadingProgress 合并，LibraryScreen 只渲染该模型。
 */
data class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val rating: Float,
    val tags: List<String>,
    val type: LibraryItemType,
    val collectedAt: String,
    val filePath: String = "",
    val progress: Float = 0f,
    val coverUrl: String? = null,
    val lastReadAt: String? = null,
    val syncStatus: LibrarySyncStatus = LibrarySyncStatus.SYNCED,

){
    fun isValid(): Boolean = id.isNotBlank() && filePath.isNotBlank()
}

enum class LibrarySyncStatus { SYNCED, SYNCING, FAILED, LOCAL_ONLY }

enum class LibraryItemType {
    TXT,
    EPUB
}
