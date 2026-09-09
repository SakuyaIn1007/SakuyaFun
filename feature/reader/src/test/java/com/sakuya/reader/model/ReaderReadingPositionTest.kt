package com.sakuya.reader.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ReaderReadingPositionTest.kt
 * 职责说明：验证跨格式阅读位置在进入持久层前会被规范化，防止异常滚动值破坏恢复逻辑。
 */
class ReaderReadingPositionTest {
    @Test
    fun normalized_clampsProgressAndKeepsChapterAnchor() {
        val position = ReaderReadingPosition(
            type = ReaderType.WENKU8,
            progress = 1.4f,
            chapterId = "chapter-12",
            chapterIndex = -3,
            chapterProgress = -0.2f,
        ).normalized

        assertEquals(1f, position.progress, 0f)
        assertEquals("chapter-12", position.chapterId)
        assertEquals(0, position.chapterIndex)
        assertEquals(0f, position.chapterProgress, 0f)
    }

    @Test
    fun normalized_preservesAValidEpubPosition() {
        val position = ReaderReadingPosition(
            type = ReaderType.EPUB,
            progress = 0.42f,
            chapterIndex = 4,
            chapterProgress = 0.75f,
        ).normalized

        assertEquals(ReaderType.EPUB, position.type)
        assertEquals(0.42f, position.progress, 0f)
        assertEquals(4, position.chapterIndex)
        assertEquals(0.75f, position.chapterProgress, 0f)
    }
}
