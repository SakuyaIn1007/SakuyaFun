package com.sakuya.reader.data

import com.sakuya.data.offline.OfflineChapter
import com.sakuya.reader.model.ReaderDocument
import com.sakuya.reader.model.Wenku8ChapterAnchor

/**
 * OfflineFullDocumentBuilder.kt
 * 职责说明：把逐章落盘的离线章节拼成会话内的全文文档，并生成指向各章标题起点的偏移锚点。
 * 执行流程：按章节顺序拼接「标题 + 空行 + 正文」，章与章之间补空行分隔；每个锚点的 offset
 * 记录拼接完成后该章标题在全文中的起点，供目录跳章精确换算位置。
 *
 * 偏移契约：这是服务端导入偏移契约（见 docs/constraints.md §1）在客户端侧的同类约定——
 * 锚点 offset 必须指向该章标题的第一个字符。偏移一旦错位，跳章不会报错，而是静默落到错误段落，
 * 因此由 OfflineFullDocumentBuilderTest 锁定。
 *
 * 边界：空列表返回空全文与空锚点，调用方据锚点为空判定「无可用离线内容」并回落到其它路径。
 */
internal fun buildOfflineFullDocument(
    title: String,
    chapters: List<OfflineChapter>,
): ReaderDocument.Wenku8Full {
    val builder = StringBuilder()
    val anchors = chapters.map { chapter ->
        // 章间补空行分隔；首章之前无分隔，因此 offset 恰好指向标题起点。
        if (builder.isNotEmpty()) builder.append("\n\n")
        val anchor = Wenku8ChapterAnchor(chapter.chapterId, chapter.title, chapter.volumeTitle, builder.length)
        builder.append(chapter.title).append("\n\n").append(chapter.content)
        anchor
    }
    return ReaderDocument.Wenku8Full(title, builder.toString(), anchors)
}
