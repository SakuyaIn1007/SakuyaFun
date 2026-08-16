package com.sakuya.reader.model


sealed interface ReaderDocument{
    val title: String

    data class Txt(
        override val title: String,
        val text: String
    ) : ReaderDocument

    data class Epub(
        override val title: String,
        val chapters: List<ReaderChapter>
    ) : ReaderDocument

    /**
     * Wenku8 连续阅读的会话内文档。
     * text 和锚点只存在于内存，ReaderViewModel 用 targetChapterId 驱动 UI 定位，不会写入 Room 或文件。
     */
    data class Wenku8Full(
        override val title: String,
        val text: String,
        val chapters: List<Wenku8ChapterAnchor>
    ) : ReaderDocument
}

data class ReaderChapter(
    val index: Int,
    val title: String,
    val content: String
)

/** 一章标题在整本 UTF-16 Kotlin 字符串中的起点；-1 表示不可靠，已在 Repository 层过滤。 */
data class Wenku8ChapterAnchor(
    val chapterId: String,
    val title: String,
    val volumeTitle: String,
    val offset: Int
)
