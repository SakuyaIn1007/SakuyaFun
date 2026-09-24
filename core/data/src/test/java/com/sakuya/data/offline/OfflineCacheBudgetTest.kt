package com.sakuya.data.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OfflineCacheBudgetTest.kt
 * 职责说明：验证离线目录的容量淘汰只移除可安全移除的书，且能真正压回预算内。
 * 执行流程：构造离线记录集合 -> 计算淘汰目标 -> 断言被选中的书目与顺序。
 *
 * 淘汰是不可逆的破坏性操作，这里逐条固定保护规则，避免后续改动放宽判据。
 */
class OfflineCacheBudgetTest {

    private val mb = 1024L * 1024L

    private fun entry(
        bookId: String,
        sizeMb: Long,
        lastAccessedAt: Long,
        status: OfflineDownloadStatus = OfflineDownloadStatus.COMPLETED,
        pinned: Boolean = false,
    ) = OfflineCacheEntry(bookId, lastAccessedAt, sizeMb * mb, pinned, status)

    @Test
    fun withinBudgetEvictsNothing() {
        val entries = listOf(entry("a", 10, 1), entry("b", 10, 2))
        assertEquals(emptyList<String>(), selectEvictions(entries, budgetBytes = 100 * mb))
    }

    @Test
    fun overBudgetEvictsLeastRecentlyAccessedFirst() {
        val entries = listOf(
            entry("recent", 30, lastAccessedAt = 300),
            entry("oldest", 30, lastAccessedAt = 100),
            entry("middle", 30, lastAccessedAt = 200),
        )
        // 90MB 超 50MB 预算：删最旧一本后剩 60MB 仍超，须继续删第二旧的，保留最近的。
        assertEquals(listOf("oldest", "middle"), selectEvictions(entries, budgetBytes = 50 * mb))
    }

    @Test
    fun stopsEvictingAsSoonAsBudgetIsSatisfied() {
        val entries = listOf(
            entry("oldest", 30, lastAccessedAt = 100),
            entry("newer", 30, lastAccessedAt = 200),
        )
        // 60MB 超 50MB 预算：删最旧一本后即为 30MB，不应继续删第二本。
        assertEquals(listOf("oldest"), selectEvictions(entries, budgetBytes = 50 * mb))
    }

    @Test
    fun exactlyAtBudgetEvictsNothing() {
        val entries = listOf(entry("a", 25, lastAccessedAt = 100), entry("b", 25, lastAccessedAt = 200))
        assertEquals(emptyList<String>(), selectEvictions(entries, budgetBytes = 50 * mb))
    }

    @Test
    fun evictsEnoughBooksToReturnUnderBudget() {
        val entries = listOf(
            entry("a", 40, lastAccessedAt = 100),
            entry("b", 40, lastAccessedAt = 200),
            entry("c", 40, lastAccessedAt = 300),
        )
        // 120MB 超 50MB 预算：必须连删两本（a、b）才能回到 40MB。
        assertEquals(listOf("a", "b"), selectEvictions(entries, budgetBytes = 50 * mb))
    }

    @Test
    fun inProgressDownloadsAreNeverEvicted() {
        val entries = listOf(
            entry("completed", 40, lastAccessedAt = 100),
            entry("downloading", 40, lastAccessedAt = 100, status = OfflineDownloadStatus.DOWNLOADING),
            entry("paused", 40, lastAccessedAt = 100, status = OfflineDownloadStatus.PAUSED),
            entry("failed", 40, lastAccessedAt = 100, status = OfflineDownloadStatus.FAILED),
            entry("queued", 40, lastAccessedAt = 100, status = OfflineDownloadStatus.QUEUED),
        )
        assertEquals(listOf("completed"), selectEvictions(entries, budgetBytes = 50 * mb))
    }

    @Test
    fun pinnedBooksSurviveEvenWhenTheyAreTheOldest() {
        val entries = listOf(
            entry("pinned-oldest", 40, lastAccessedAt = 1, pinned = true),
            entry("unpinned", 40, lastAccessedAt = 200),
        )
        assertEquals(listOf("unpinned"), selectEvictions(entries, budgetBytes = 50 * mb))
    }

    @Test
    fun currentlyReadingBookIsExcludedSoReadingIsNotInterrupted() {
        val entries = listOf(
            entry("reading", 40, lastAccessedAt = 1),
            entry("other", 40, lastAccessedAt = 200),
        )
        assertEquals(listOf("other"), selectEvictions(entries, budgetBytes = 50 * mb, excludeBookId = "reading"))
    }

    @Test
    fun inProgressDownloadsCountTowardBudgetEvenThoughTheyCannotBeEvicted() {
        val entries = listOf(
            entry("downloading", 60, lastAccessedAt = 500, status = OfflineDownloadStatus.DOWNLOADING),
            entry("completed", 40, lastAccessedAt = 100),
        )
        // 进行中的 60MB 同样占磁盘：总量 100MB 超出 50MB 预算，须淘汰已完成的 40MB。
        assertEquals(listOf("completed"), selectEvictions(entries, budgetBytes = 50 * mb))
    }

    @Test
    fun whenOnlyProtectedBooksRemainNothingIsEvictedEvenIfStillOverBudget() {
        val entries = listOf(
            entry("pinned", 90, lastAccessedAt = 1, pinned = true),
            entry("downloading", 90, lastAccessedAt = 2, status = OfflineDownloadStatus.DOWNLOADING),
        )
        // 宁可超出预算，也不删除受保护的内容；此时不返回任何淘汰目标。
        assertTrue(selectEvictions(entries, budgetBytes = 50 * mb).isEmpty())
    }

    @Test
    fun alreadyDeletedOrEmptyAccountEvictsNothing() {
        assertEquals(emptyList<String>(), selectEvictions(emptyList(), budgetBytes = 0))
    }
}
