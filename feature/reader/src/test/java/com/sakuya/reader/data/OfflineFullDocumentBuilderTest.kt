package com.sakuya.reader.data

import com.sakuya.data.offline.OfflineChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OfflineFullDocumentBuilderTest.kt
 * 职责说明：验证离线章节拼全文的偏移契约——锚点 offset 必须指向该章标题在全文中的起点。
 * 执行流程：构造多章离线数据 -> 拼全文 -> 断言每个锚点处文本以章节标题开头、偏移严格递增且在文本范围内。
 * 背景：偏移一旦错位不会抛异常，而是让离线阅读的跳章静默落到错误段落，故用测试锁定。
 */
class OfflineFullDocumentBuilderTest {

    private fun chapter(id: String, title: String, volume: String = "第一卷", content: String) =
        OfflineChapter(chapterId = id, title = title, volumeTitle = volume, order = 0, content = content)

    @Test
    fun anchorsPointAtChapterTitleStart() {
        val document = buildOfflineFullDocument(
            title = "离线小说",
            chapters = listOf(
                chapter("c1", "第一章 起点", content = "正文一"),
                chapter("c2", "第二章 继续", content = "正文二"),
            ),
        )

        document.chapters.forEach { anchor ->
            assertTrue(
                "锚点 ${anchor.chapterId} 的偏移未指向章节标题起点",
                document.text.startsWith(anchor.title, anchor.offset),
            )
        }
    }

    @Test
    fun anchorsAreStrictlyIncreasingAndWithinText() {
        val document = buildOfflineFullDocument(
            title = "离线小说",
            chapters = listOf(
                chapter("c1", "第一章", content = "正文一"),
                chapter("c2", "第二章", content = "正文二"),
                chapter("c3", "第三章", content = "正文三"),
            ),
        )

        val offsets = document.chapters.map { it.offset }
        // 排序后与自身相等 + 去重后长度不变 => 严格递增，且偏移唯一可作章节标识。
        assertEquals(offsets.sorted(), offsets)
        assertEquals(offsets.distinct(), offsets)
        assertTrue(offsets.all { it in 0 until document.text.length })
    }

    @Test
    fun preservesChapterIdentityAndVolume() {
        val document = buildOfflineFullDocument(
            title = "离线小说",
            chapters = listOf(chapter("c1", "外传一 见习生的初恋", volume = "恋爱插话集第一弹", content = "正文")),
        )

        val anchor = document.chapters.single()
        assertEquals("c1", anchor.chapterId)
        assertEquals("外传一 见习生的初恋", anchor.title)
        assertEquals("恋爱插话集第一弹", anchor.volumeTitle)
        assertEquals("离线小说", document.title)
    }

    @Test
    fun emptyChaptersProduceEmptyDocument() {
        val document = buildOfflineFullDocument(title = "空", chapters = emptyList())

        assertEquals("", document.text)
        assertTrue(document.chapters.isEmpty())
    }
}
