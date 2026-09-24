package com.sakuya.data.content

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ContentCachePolicyTest.kt
 * 职责说明：锁定内容接口的缓存时长契约，并保证错误响应与未知路径不会被缓存。
 * 执行流程：给定路径与状态码 -> 取策略 -> 断言具体 Cache-Control 字面量。
 *
 * 这里刻意使用字面量而非常量：缓存时长是会被后端下架节奏约束的对外契约，
 * 改动必须显式反映到测试中，不能随常量一起静默漂移。
 */
class ContentCachePolicyTest {

    @Test
    fun chapterContentIsCachedLongestBecausePublishedChaptersAreImmutable() {
        assertEquals(
            "max-age=604800",
            ContentCachePolicy.cacheControlFor("/content/chapters/chapter-1", 200),
        )
    }

    @Test
    fun fullContentUsesShortTtlBecauseNewChaptersAppendToIt() {
        assertEquals(
            "max-age=3600",
            ContentCachePolicy.cacheControlFor("/content/novels/book-1/full-content", 200),
        )
    }

    @Test
    fun chapterIndexAndNovelMetadataUseTheirOwnTtls() {
        assertEquals(
            "max-age=1800",
            ContentCachePolicy.cacheControlFor("/content/novels/book-1/chapters", 200),
        )
        assertEquals(
            "max-age=3600",
            ContentCachePolicy.cacheControlFor("/content/novels/book-1", 200),
        )
    }

    @Test
    fun searchUsesShortTtlBecauseResultSetIsVolatile() {
        assertEquals(
            "max-age=300",
            ContentCachePolicy.cacheControlFor("/content/novels", 200),
        )
    }

    @Test
    fun unknownPathsAreNeverCached() {
        assertEquals(ContentCachePolicy.NO_STORE, ContentCachePolicy.cacheControlFor("/content/novels/book-1/cover", 200))
        assertEquals(ContentCachePolicy.NO_STORE, ContentCachePolicy.cacheControlFor("/content", 200))
        assertEquals(ContentCachePolicy.NO_STORE, ContentCachePolicy.cacheControlFor("/user/profile", 200))
        assertEquals(ContentCachePolicy.NO_STORE, ContentCachePolicy.cacheControlFor("/content/novels/book-1/unknown", 200))
    }

    @Test
    fun errorResponsesAreNeverCachedSoRecoveryIsVisibleImmediately() {
        // 下架返回 403、未入库返回 404；若被缓存，恢复后仍会读到失败结果。
        assertEquals(ContentCachePolicy.NO_STORE, ContentCachePolicy.cacheControlFor("/content/chapters/chapter-1", 403))
        assertEquals(ContentCachePolicy.NO_STORE, ContentCachePolicy.cacheControlFor("/content/chapters/chapter-1", 404))
        assertEquals(ContentCachePolicy.NO_STORE, ContentCachePolicy.cacheControlFor("/content/chapters/chapter-1", 500))
        assertEquals(ContentCachePolicy.NO_STORE, ContentCachePolicy.cacheControlFor("/content/novels/book-1/full-content", 503))
    }

    @Test
    fun trailingSlashAndMissingLeadingSlashStillMatch() {
        assertEquals(
            "max-age=604800",
            ContentCachePolicy.cacheControlFor("content/chapters/chapter-1/", 200),
        )
    }
}
