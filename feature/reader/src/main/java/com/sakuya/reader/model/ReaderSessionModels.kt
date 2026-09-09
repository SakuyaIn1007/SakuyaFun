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
    /** 章节标识用于 Wenku8/EPUB 在全局比例变化后仍能优先定位到同一章。 */
    val chapterId: String? = null,
    val chapterProgress: Float = 0f,
)

/**
 * ReaderReadingPosition.kt
 * 职责说明：描述跨阅读器实现可持久化的阅读位置。
 * 执行流程：正文组件回传位置 -> ViewModel 防抖保存 -> Repository 映射为 Room 数据；
 * 下次打开时先使用章节定位，缺失章节信息再回退到全局比例，避免字体大小变化造成明显跳页。
 */
data class ReaderReadingPosition(
    val type: ReaderType = ReaderType.TXT,
    val progress: Float = 0f,
    val chapterId: String? = null,
    val chapterIndex: Int = 0,
    val chapterProgress: Float = 0f,
) {
    val normalized: ReaderReadingPosition
        get() = copy(
            progress = progress.coerceIn(0f, 1f),
            chapterIndex = chapterIndex.coerceAtLeast(0),
            chapterProgress = chapterProgress.coerceIn(0f, 1f),
        )
}

enum class ReaderTheme { SYSTEM, LIGHT, DARK, SEPIA }

/**
 * 阅读页面的互斥内容状态。
 * ViewModel 只产生该状态，Composable 根据状态渲染加载、正文、错误或远端降级提示，
 * 从而避免同时依赖多个布尔值造成不可达或冲突的 UI。
 */
sealed interface ReaderSessionState {
    data object Idle : ReaderSessionState
    data object Loading : ReaderSessionState
    data object Reading : ReaderSessionState
    data class Error(val message: String) : ReaderSessionState
}

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
    data object FileTooLarge : ReaderParseError
    data object DownloadFailed : ReaderParseError
    data object RemoteTimeout : ReaderParseError
    data object EmptyDocument : ReaderParseError
    data class InvalidContent(val detail: String? = null) : ReaderParseError
}
