package com.sakuya.reader.data

import com.sakuya.reader.model.Wenku8ChapterAnchor

/**
 * Wenku8ChapterAnchorMapper.kt
 * 职责说明：将网络层全文锚点转换为阅读器可安全使用的领域模型。
 * 执行流程：校验章节 ID、标题和偏移范围 -> 丢弃不可靠数据 -> 按全文偏移升序输出给连续阅读 UI。
 */
internal fun mapReadableAnchors(
    values: List<Wenku8ChapterAnchorDto>,
    textLength: Int,
): List<Wenku8ChapterAnchor> = values.mapNotNull { dto ->
    val id = dto.chapterId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    val title = dto.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    if (dto.offset !in 0 until textLength) return@mapNotNull null
    Wenku8ChapterAnchor(id, title, dto.volumeTitle.orEmpty(), dto.offset)
}.sortedBy { it.offset }
