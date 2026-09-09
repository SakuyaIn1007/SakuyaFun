package com.sakuya.reader.data

import com.sakuya.data.content.ContentChapterAnchorDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Wenku8ChapterAnchorMapperTest.kt
 * 职责说明：验证连续阅读只接收能够安全定位的上游章节锚点。
 * 执行流程：构造乱序和无效的网关 DTO -> 映射 -> 断言正文范围校验与位置排序。
 */
class Wenku8ChapterAnchorMapperTest {
    @Test
    fun mapsOnlyInRangeAnchorsAndOrdersThemByOffset() {
        val anchors = mapReadableAnchors(
            values = listOf(
                ContentChapterAnchorDto(chapterId = "second", title = "第二章", offset = 20),
                ContentChapterAnchorDto(chapterId = "missing", title = "缺失", offset = -1),
                ContentChapterAnchorDto(chapterId = "outside", title = "越界", offset = 40),
                ContentChapterAnchorDto(chapterId = "first", title = "第一章", volumeTitle = "第一卷", offset = 0),
                ContentChapterAnchorDto(chapterId = "", title = "无效", offset = 5),
            ),
            textLength = 40,
        )

        assertEquals(listOf("first", "second"), anchors.map { it.chapterId })
        assertEquals("第一卷", anchors.first().volumeTitle)
    }
}
