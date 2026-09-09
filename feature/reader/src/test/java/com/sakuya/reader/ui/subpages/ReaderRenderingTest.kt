package com.sakuya.reader.ui.subpages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ReaderRenderingTest.kt
 * 职责说明：验证长文本分块不会改变内容顺序，以及 EPUB 清洗会移除可执行或外部加载入口。
 */
class ReaderRenderingTest {
    @Test
    fun splitTxtIntoBlocks_preservesAllCharactersAndRespectsBound() {
        val source = "第一段内容\n\n" + "x".repeat(31) + "\n\n最后一段"

        val blocks = splitTxtIntoBlocks(source, maxBlockCharacters = 12)

        assertEquals(source, blocks.joinToString(separator = ""))
        assertTrue(blocks.all { it.length <= 12 })
    }

    @Test
    fun sanitizeEpubHtml_removesScriptsEventsAndExternalResources() {
        val result = sanitizeEpubHtml(
            "<script>alert(1)</script><p onclick=\"run()\"><a href=\"https://example.com\">正文</a></p><img src=\"file:///secret\">",
        )

        assertFalse(result.contains("script", ignoreCase = true))
        assertFalse(result.contains("onclick", ignoreCase = true))
        assertFalse(result.contains("https://", ignoreCase = true))
        assertFalse(result.contains("img", ignoreCase = true))
        assertTrue(result.contains("正文"))
    }
}
