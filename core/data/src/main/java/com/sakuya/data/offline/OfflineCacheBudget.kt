package com.sakuya.data.offline

/**
 * OfflineCacheBudget.kt
 * 职责说明：离线目录的容量预算与 LRU 淘汰选择，全部为纯函数以便单元测试。
 * 执行流程：汇总占用 -> 超出预算时按最久未读排序 -> 整本淘汰已完成且未被保护的书。
 *
 * 淘汰必须整本进行：章节文件与 Room 索引按书组织，删一半会留下无法阅读也无法清理的残骸。
 */

/** 正文为纯文本，单本约 2-5 MiB，512 MiB 约可容纳一百本以上。 */
const val DEFAULT_OFFLINE_BUDGET_BYTES = 512L * 1024L * 1024L

/** 一条离线缓存记录，供淘汰计算使用。 */
internal data class OfflineCacheEntry(
    val bookId: String,
    val lastAccessedAt: Long,
    val sizeBytes: Long,
    val pinned: Boolean,
    val status: OfflineDownloadStatus,
)

/**
 * 选出需要淘汰的 bookId，按淘汰优先级返回。
 *
 * 保护规则（任一命中即不淘汰）：
 * - 非 COMPLETED：下载中/排队/暂停/失败的书删掉会让用户丢掉正在进行的进度；
 * - pinned：用户显式钉住，不允许自动移除；
 * - excludeBookId：当前正在阅读的书，淘汰它会导致阅读中断。
 *
 * @param entries 当前账号下的全部离线记录（含进行中的任务，它们同样占用磁盘）。
 */
internal fun selectEvictions(
    entries: List<OfflineCacheEntry>,
    budgetBytes: Long,
    excludeBookId: String? = null,
): List<String> {
    // 进行中的下载也占用磁盘，必须计入总量，否则预算会被低估。
    var remaining = entries.sumOf { it.sizeBytes }
    if (remaining <= budgetBytes) return emptyList()

    val candidates = entries
        .filter { it.status == OfflineDownloadStatus.COMPLETED }
        .filter { !it.pinned }
        .filter { it.bookId != excludeBookId }
        .sortedBy { it.lastAccessedAt }

    val selected = mutableListOf<String>()
    for (candidate in candidates) {
        if (remaining <= budgetBytes) break
        selected += candidate.bookId
        remaining -= candidate.sizeBytes
    }
    return selected
}
