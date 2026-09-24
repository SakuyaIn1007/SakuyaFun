package com.sakuya.reader.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FileDownloaderCacheTest.kt
 * 职责说明：验证导入缓存按 TTL 失效，且尺寸预检能在落盘前拦下超大文件。
 * 执行流程：构造时间戳与长度 -> 调用纯判定函数 -> 断言复用与超限结论。
 *
 * 这两个判定原先内联在下载流程里，导致「同 URL 永久返回旧文件」的缺陷无法被测试发现。
 */
class FileDownloaderCacheTest {

    private val now = 1_700_000_000_000L

    @Test
    fun missingFileIsNeverReused() {
        assertFalse(isDownloadCacheReusable(lastModified = 0L, now = now))
    }

    @Test
    fun freshFileIsReused() {
        assertTrue(isDownloadCacheReusable(lastModified = now - 60_000L, now = now))
    }

    @Test
    fun expiredFileIsRedownloaded() {
        // 这正是原缺陷：文件存在即复用，导致服务端替换内容后仍读到旧文件。
        assertFalse(isDownloadCacheReusable(lastModified = now - DOWNLOAD_CACHE_TTL_MILLIS - 1L, now = now))
    }

    @Test
    fun ttlBoundaryIsTreatedAsExpired() {
        assertFalse(isDownloadCacheReusable(lastModified = now - DOWNLOAD_CACHE_TTL_MILLIS, now = now))
        assertTrue(isDownloadCacheReusable(lastModified = now - DOWNLOAD_CACHE_TTL_MILLIS + 1L, now = now))
    }

    @Test
    fun futureTimestampIsTreatedAsFreshToSurviveClockSkew() {
        // 时钟回拨会让时间戳超前于当前时间，此时不应反复重新下载。
        assertTrue(isDownloadCacheReusable(lastModified = now + 600_000L, now = now))
    }

    @Test
    fun declaredLengthOverLimitIsRejectedBeforeWriting() {
        val maxBytes = 15L * 1024L * 1024L
        assertTrue(exceedsMaxBytes(contentLength = maxBytes + 1L, maxBytes = maxBytes))
    }

    @Test
    fun lengthWithinLimitOrUnknownIsAccepted() {
        val maxBytes = 15L * 1024L * 1024L
        assertFalse(exceedsMaxBytes(contentLength = maxBytes, maxBytes = maxBytes))
        // -1 表示分块传输未声明长度，此时不能误判为超限，交由落盘后的兜底校验处理。
        assertFalse(exceedsMaxBytes(contentLength = -1L, maxBytes = maxBytes))
    }
}
