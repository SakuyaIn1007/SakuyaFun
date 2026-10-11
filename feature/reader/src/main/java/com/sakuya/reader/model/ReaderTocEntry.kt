package com.sakuya.reader.model

/**
 * ReaderTocEntry.kt
 * 职责说明：把两种带章节的文档（Wenku8 全文、EPUB）统一映射为目录面板可渲染的条目。
 * 执行流程：ReaderScreen 从 ReaderUiState.document 构建条目 -> 目录面板回传点击
 * -> ViewModel 按 chapterId / chapterIndex 定位，条目本身不含任何业务判断。
 */
data class ReaderTocEntry(
    val title: String,
    /** Wenku8 的卷名，用于表达「卷 > 章」层级；EPUB 为 null。 */
    val subtitle: String? = null,
    /** Wenku8 定位键；EPUB 为 null。 */
    val chapterId: String? = null,
    /** EPUB 定位键；Wenku8 也填入原始索引，便于回退定位。 */
    val chapterIndex: Int = -1,
    val isCurrent: Boolean = false,
) {
    /** LazyColumn 稳定键：Wenku8 用章节 ID，EPUB 退回索引。 */
    val key: String get() = chapterId ?: "index:$chapterIndex"
}

/**
 * 构建目录条目；TXT 没有可靠章节边界（见 TxtReaderContent），因此与空文档一同返回空列表，
 * 调用方据此隐藏「目录」入口。不可靠锚点（offset == -1）由 Repository 层预先过滤，此处不做二次判断。
 */
fun buildTocEntries(
    document: ReaderDocument?,
    currentChapterId: String?,
    currentChapterIndex: Int,
): List<ReaderTocEntry> = when (document) {
    is ReaderDocument.Wenku8Full -> document.chapters.mapIndexed { index, anchor ->
        ReaderTocEntry(
            title = anchor.title,
            subtitle = anchor.volumeTitle.takeIf { it.isNotBlank() },
            chapterId = anchor.chapterId,
            chapterIndex = index,
            isCurrent = anchor.chapterId == currentChapterId,
        )
    }
    is ReaderDocument.Epub -> document.chapters.map { chapter ->
        ReaderTocEntry(
            title = chapter.title,
            chapterIndex = chapter.index,
            isCurrent = chapter.index == currentChapterIndex,
        )
    }
    else -> emptyList()
}
