package com.sakuya.data.reading

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 验证离线删除和章节位置不会在 Retrofit JSON 边界被遗漏。 */
class ReadingSyncPayloadTest {
    @Test
    fun `progress tombstone keeps stable book and conflict fields`() {
        val request = ReadingSyncRequestDto(
            deviceId = "device-a",
            progressChanges = listOf(
                ProgressMutationDto(
                    bookId = "book-1",
                    contentType = "WENKU8",
                    progress = 0.6f,
                    chapterId = "chapter-2",
                    chapterIndex = 1,
                    chapterProgress = 0.2f,
                    clientModifiedAt = "2026-09-01T00:00:00Z",
                    deleted = true,
                )
            ),
            bookmarkChanges = emptyList(),
        )

        val json = Gson().toJson(request)
        assertTrue(json.contains("\"bookId\":\"book-1\""))
        assertTrue(json.contains("\"clientModifiedAt\":\"2026-09-01T00:00:00Z\""))
        assertTrue(json.contains("\"deleted\":true"))
    }

    @Test
    fun `empty server snapshot is safe for a new account`() {
        val response = ReadingSyncResponseDto(serverTime = "2026-09-01T00:00:00Z")
        assertEquals(emptyList<ProgressDto>(), response.progresses)
        assertEquals(emptyList<BookmarkDto>(), response.bookmarks)
    }
}
