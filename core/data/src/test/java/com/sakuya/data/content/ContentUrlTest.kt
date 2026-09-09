package com.sakuya.data.content

import com.sakuya.data.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 验证统一内容封面同时兼容站内路径和完整 CDN 地址，避免迁移期生成无效双 Host URL。 */
class ContentUrlTest {
    @Test
    fun relativePathUsesApiBaseUrl() {
        assertEquals(
            BuildConfig.API_BASE_URL.trimEnd('/') + "/content/novels/book-1/cover",
            resolveContentUrl("/content/novels/book-1/cover"),
        )
    }

    @Test
    fun absoluteAndBlankUrlsArePreservedSafely() {
        assertEquals("https://cdn.example.test/cover.webp", resolveContentUrl("https://cdn.example.test/cover.webp"))
        assertNull(resolveContentUrl("  "))
        assertNull(resolveContentUrl(null))
    }
}
