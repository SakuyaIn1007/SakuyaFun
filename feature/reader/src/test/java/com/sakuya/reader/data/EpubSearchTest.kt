package com.sakuya.reader.data

import com.sakuya.reader.model.ReaderChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 验证 EPUB 搜索会过滤 HTML、保留真实章节标题，并返回可跳转章节索引。 */
class EpubSearchTest {
    @Test fun searchesPlainTextAndLimitsShortQueries() {
        val chapters = listOf(
            ReaderChapter(0, "序章", "<html><body><p>没有目标</p></body></html>"),
            ReaderChapter(1, "雨夜", "<h1>雨夜</h1><p>她终于找到了那封信。</p>"),
        )
        assertTrue(EpubSearch.search(chapters, "信").isEmpty())
        val result = EpubSearch.search(chapters, "那封信").single()
        assertEquals(1, result.chapterIndex)
        assertEquals("雨夜", result.chapterTitle)
        assertTrue(result.excerpt.contains("那封信"))
    }
}
