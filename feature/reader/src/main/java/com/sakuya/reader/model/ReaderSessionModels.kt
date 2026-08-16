package com.sakuya.reader.model

/**
 * ReaderSessionModels.kt
 * 职责说明：定义阅读会话中的书签、主题偏好和文件解析结果。
 * 执行流程：ReaderRepository 产生 ReaderOpenResult 与 ReaderBookmark，ReaderViewModel 将它们写入 ReaderUiState。
 */
data class ReaderBookmark(
    val id: String,
    val title: String,
    val progress: Float,
    val note: String,
    val createdAt: Long,
)

enum class ReaderTheme { SYSTEM, LIGHT, DARK, SEPIA }

sealed interface ReaderOpenResult {
    data class Success(val document: ReaderDocument) : ReaderOpenResult
    data class Failure(val error: ReaderParseError) : ReaderOpenResult
}

/**
 * 远端小说打开结果将“全文成功”“单章降级”“完全失败”分开，
 * 使 ViewModel 能保留连续阅读进度，同时对用户明确说明降级原因。
 */
sealed interface Wenku8NovelOpenResult {
    data class Full(val document: ReaderDocument.Wenku8Full) : Wenku8NovelOpenResult
    data class ChapterFallback(val document: ReaderDocument.Txt, val notice: String) : Wenku8NovelOpenResult
    data class Failure(val error: ReaderParseError) : Wenku8NovelOpenResult
}

sealed interface ReaderParseError {
    data object UnsupportedFormat : ReaderParseError
    data object FileNotFound : ReaderParseError
    data object DownloadFailed : ReaderParseError
    data object EmptyDocument : ReaderParseError
    data class InvalidContent(val detail: String? = null) : ReaderParseError
}
