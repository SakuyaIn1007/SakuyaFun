package com.sakuya.reader.viewmodel

import com.sakuya.reader.model.ReaderChapter
import com.sakuya.reader.model.ReaderDocument
import com.sakuya.reader.model.ReaderType
import com.sakuya.reader.model.Wenku8ChapterAnchor
import com.sakuya.reader.model.buildTocEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ReaderChapterJumpTest.kt
 * 职责说明：验证目录跳章的两段纯逻辑——位置换算 resolveChapterJump 与条目构建 buildTocEntries，
 * 覆盖锚点缺失、索引越界、TXT 无章节等边界，避免 UI 层出现无法定位或越界崩溃。
 */
class ReaderChapterJumpTest {

    /** 锚点 offset 是整本 UTF-16 字符串下标，此处用等长文本精确验证换算。 */
    private val wenku8Document = ReaderDocument.Wenku8Full(
        title = "测试小说",
        text = "0123456789ABCDEFGHIJ",
        chapters = listOf(
            Wenku8ChapterAnchor("c1", "第1章 开始", "第一卷", offset = 0),
            Wenku8ChapterAnchor("c2", "第2章 继续", "第一卷", offset = 10),
        ),
    )

    private val epubDocument = ReaderDocument.Epub(
        title = "测试 EPUB",
        chapters = listOf(
            ReaderChapter(0, "第一章", "a"),
            ReaderChapter(1, "第二章", "b"),
            ReaderChapter(2, "第三章", "c"),
            ReaderChapter(3, "第四章", "d"),
        ),
    )

    @Test
    fun wenku8Anchor_resolvesProgressFromOffset() {
        val position = resolveChapterJump(wenku8Document, chapterId = "c2", chapterIndex = -1)

        assertEquals(10f / wenku8Document.text.length, position!!.progress, 0.0001f)
        assertEquals("c2", position.chapterId)
        assertEquals(ReaderType.WENKU8, position.type)
        assertEquals(0f, position.chapterProgress, 0.0001f)
    }

    @Test
    fun wenku8UnknownChapter_returnsNull() {
        assertNull(resolveChapterJump(wenku8Document, chapterId = "missing", chapterIndex = 0))
    }

    @Test
    fun wenku8UnreliableAnchor_returnsNull() {
        // offset == -1 表示锚点不可靠，跳过而非定位到错误位置。
        val document = wenku8Document.copy(
            chapters = listOf(Wenku8ChapterAnchor("bad", "坏锚点", "第一卷", offset = -1)),
        )

        assertNull(resolveChapterJump(document, chapterId = "bad", chapterIndex = 0))
    }

    @Test
    fun epubIndex_resolvesProgressFromIndex() {
        val position = resolveChapterJump(epubDocument, chapterId = null, chapterIndex = 2)

        assertEquals(2f / 4, position!!.progress, 0.0001f)
        assertEquals(2, position.chapterIndex)
        assertEquals(ReaderType.EPUB, position.type)
    }

    @Test
    fun epubIndexOutOfRange_returnsNull() {
        assertNull(resolveChapterJump(epubDocument, chapterId = null, chapterIndex = 99))
    }

    @Test
    fun txt_returnsNull() {
        assertNull(resolveChapterJump(ReaderDocument.Txt("本地文本", "正文"), chapterId = null, chapterIndex = 0))
    }

    @Test
    fun buildTocEntries_wenku8CarriesVolumeAndMarksCurrent() {
        val entries = buildTocEntries(wenku8Document, currentChapterId = "c2", currentChapterIndex = 0)

        assertEquals(2, entries.size)
        assertEquals("第一卷", entries[1].subtitle)
        assertTrue(entries[1].isCurrent)
        assertEquals("c2", entries[1].key)
    }

    @Test
    fun buildTocEntries_epubMarksCurrentByIndex() {
        val entries = buildTocEntries(epubDocument, currentChapterId = null, currentChapterIndex = 3)

        assertEquals(4, entries.size)
        assertTrue(entries[3].isCurrent)
        assertEquals("index:3", entries[3].key)
    }

    @Test
    fun buildTocEntries_txtAndNullReturnEmpty() {
        assertTrue(buildTocEntries(ReaderDocument.Txt("本地文本", "正文"), null, 0).isEmpty())
        assertTrue(buildTocEntries(null, null, 0).isEmpty())
    }
}
