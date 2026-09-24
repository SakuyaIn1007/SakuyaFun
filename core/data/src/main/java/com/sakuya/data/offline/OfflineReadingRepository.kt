package com.sakuya.data.offline

import android.content.Context
import androidx.room.withTransaction
import com.sakuya.data.content.ContentApiService
import com.sakuya.data.local.AppDatabase
import com.sakuya.data.local.SessionManager
import com.sakuya.data.local.dao.OfflineDownloadDao
import com.sakuya.data.local.entity.OfflineChapterEntity
import com.sakuya.data.local.entity.OfflineDownloadEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

enum class OfflineDownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED }

data class OfflineDownloadState(
    val bookId: String,
    val title: String,
    val status: OfflineDownloadStatus,
    val completedChapters: Int,
    val totalChapters: Int,
    val downloadedBytes: Long,
    val errorMessage: String?,
) {
    val progress: Float get() = if (totalChapters == 0) 0f else completedChapters.toFloat() / totalChapters
}

data class OfflineChapter(val chapterId: String, val title: String, val volumeTitle: String, val order: Int, val content: String)

/**
 * OfflineReadingRepository.kt
 * 职责说明：管理在线书离线任务、章节文件和账号隔离，并提供空间清理及离线正文读取。
 * 执行流程：任务状态写 Room -> 下载正文原子写入应用私有目录 -> Room 记录文件索引；删除时先删文件再清索引。
 */
@Singleton
class OfflineReadingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val dao: OfflineDownloadDao,
    private val sessionManager: SessionManager,
    private val contentApi: ContentApiService,
    private val signals: OfflineDownloadSignals,
    private val evictions: OfflineEvictionEvents,
) {
    /**
     * 淘汰与下载都会改写同一本书的文件，必须串行化，否则可能删掉正在写入的章节。
     * 单实例 Mutex 即可覆盖：本仓库是应用级单例。
     */
    private val evictionMutex = Mutex()

    suspend fun observe(bookId: String): Flow<OfflineDownloadState?> {
        val owner = sessionManager.currentUserId() ?: return flowOf(null)
        return dao.observe(owner, canonical(bookId)).map { it?.toModel() }
    }

    suspend fun enqueue(bookId: String, title: String) {
        val owner = requireOwner()
        val rawId = canonical(bookId)
        val existing = dao.get(owner, rawId)
        dao.upsertDownload((existing ?: OfflineDownloadEntity(owner, rawId, title, OfflineDownloadStatus.QUEUED.name)).copy(
            title = title.ifBlank { existing?.title.orEmpty() }, status = OfflineDownloadStatus.QUEUED.name,
            errorMessage = null, updatedAt = System.currentTimeMillis(),
        ))
        signals.request()
    }

    suspend fun pause(bookId: String) = updateStatus(bookId, OfflineDownloadStatus.PAUSED)
    suspend fun resume(bookId: String) { updateStatus(bookId, OfflineDownloadStatus.QUEUED); signals.request() }

    suspend fun retry(bookId: String) { updateStatus(bookId, OfflineDownloadStatus.QUEUED, null); signals.request() }

    suspend fun delete(bookId: String) = withContext(Dispatchers.IO) {
        val owner = requireOwner(); val rawId = canonical(bookId)
        dao.chapters(owner, rawId).forEach { runCatching { File(it.filePath).delete() } }
        database.withTransaction { dao.deleteChapters(owner, rawId); dao.deleteDownload(owner, rawId) }
        bookDirectory(owner, rawId).deleteRecursively()
    }

    suspend fun totalBytes(): Long = withContext(Dispatchers.IO) {
        val owner = requireOwner()
        File(context.filesDir, "offline-reading/${safe(owner)}").walkTopDown().filter(File::isFile).sumOf(File::length)
    }

    suspend fun readChapters(bookId: String): List<OfflineChapter> = withContext(Dispatchers.IO) {
        val owner = sessionManager.currentUserId() ?: return@withContext emptyList()
        val rawId = canonical(bookId)
        val chapters = dao.chapters(owner, rawId).mapNotNull { row ->
            runCatching { OfflineChapter(row.chapterId, row.title, row.volumeTitle, row.chapterOrder, File(row.filePath).readText()) }.getOrNull()
        }
        // 这是 LRU 的数据来源：只有真正读过书才刷新访问时间，避免「下载即算访问」。
        if (chapters.isNotEmpty()) dao.updateLastAccessed(owner, rawId, System.currentTimeMillis())
        chapters
    }

    /** 用户显式钉住的离线内容不参与自动淘汰。 */
    suspend fun setPinned(bookId: String, pinned: Boolean) {
        val owner = requireOwner()
        dao.updatePinned(owner, canonical(bookId), pinned)
    }

    /**
     * 清空当前账号的全部离线内容，返回移除的书目数量。
     * 这是设置页的显式动作：离线内容是用户主动下载的资产，只能由用户手动触发清空。
     */
    suspend fun clearAllOfflineContent(): Int = withContext(Dispatchers.IO) {
        val owner = requireOwner()
        val bookIds = dao.all(owner).map { it.bookId }
        evictionMutex.withLock { bookIds.forEach { id -> delete(id) } }
        bookIds.size
    }

    /**
     * 把离线目录压回预算之内，返回被移除的书目。
     *
     * 在下载完成之后调用：此时新增占用才算真正落定。
     * 正在阅读的书通过 excludeBookId 排除，避免淘汰中断阅读。
     */
    suspend fun enforceBudget(
        budgetBytes: Long = DEFAULT_OFFLINE_BUDGET_BYTES,
        excludeBookId: String? = null,
    ): List<String> = withContext(Dispatchers.IO) {
        val owner = sessionManager.currentUserId() ?: return@withContext emptyList()
        evictionMutex.withLock {
            val entries = dao.all(owner).map { row ->
                OfflineCacheEntry(
                    bookId = row.bookId,
                    lastAccessedAt = row.lastAccessedAt,
                    sizeBytes = row.downloadedBytes,
                    pinned = row.pinned,
                    status = runCatching { OfflineDownloadStatus.valueOf(row.status) }.getOrDefault(OfflineDownloadStatus.FAILED),
                )
            }
            val targets = selectEvictions(entries, budgetBytes, excludeBookId)
            // 先取标题：删除后记录已不存在，通知里需要可读的书名。
            val titles = targets.associateWith { id -> dao.get(owner, id)?.title.orEmpty() }
            // 复用既有 delete：它已正确处理「先删文件再清索引」并清空书籍目录。
            targets.forEach { id -> delete(id) }
            val removed = targets.map { id -> titles[id].orEmpty().ifBlank { id } }
            // 通知而非静默删除：用户需要知道离线内容为何消失。
            if (removed.isNotEmpty()) evictions.emit(removed)
            removed
        }
    }

    /** 单个任务串行逐章下载，暂停只在章节边界生效；已落盘章节通过 ID 跳过，因此重试具备幂等性。 */
    internal suspend fun execute(task: OfflineDownloadEntity) = withContext(Dispatchers.IO) {
        val current = dao.get(task.ownerId, task.bookId) ?: return@withContext
        if (current.status != OfflineDownloadStatus.QUEUED.name && current.status != OfflineDownloadStatus.DOWNLOADING.name && current.status != OfflineDownloadStatus.FAILED.name) return@withContext
        try {
            dao.upsertDownload(current.copy(status = OfflineDownloadStatus.DOWNLOADING.name, errorMessage = null, updatedAt = System.currentTimeMillis()))
            val indexResponse = contentApi.chapters(task.bookId)
            val indexBody = indexResponse.body()
            if (!indexResponse.isSuccessful || indexBody == null || !indexBody.isSuccess()) error(indexBody?.message ?: "目录下载失败")
            val items = indexBody.data?.volumes.orEmpty().flatMap { volume ->
                volume.chapters.map { chapter -> Triple(volume.title, chapter, chapter.order) }
            }.sortedBy { it.third }
            if (items.isEmpty()) error("目录为空，无法离线下载")
            val existing = dao.chapters(task.ownerId, task.bookId).associateBy { it.chapterId }.toMutableMap()
            dao.upsertDownload((dao.get(task.ownerId, task.bookId) ?: current).copy(totalChapters = items.size, completedChapters = existing.size, downloadedBytes = existing.values.sumOf { it.byteSize }))
            items.forEachIndexed { index, (volumeTitle, chapter, _) ->
                if (sessionManager.currentUserId() != task.ownerId) return@withContext
                if (dao.get(task.ownerId, task.bookId)?.status == OfflineDownloadStatus.PAUSED.name) return@withContext
                if (existing.containsKey(chapter.id)) return@forEachIndexed
                val response = contentApi.chapter(chapter.id, task.bookId)
                val body = response.body()
                if (!response.isSuccessful || body == null || !body.isSuccess()) error(body?.message ?: "章节 ${chapter.title} 下载失败")
                val text = body.data?.content.orEmpty()
                if (text.isBlank()) error("章节 ${chapter.title} 正文为空")
                val target = File(bookDirectory(task.ownerId, task.bookId), "${safe(chapter.id)}.txt")
                target.parentFile?.mkdirs()
                val temporary = File(target.parentFile, target.name + ".part")
                temporary.writeText(text)
                if (!temporary.renameTo(target)) { temporary.copyTo(target, overwrite = true); temporary.delete() }
                val row = OfflineChapterEntity(task.ownerId, task.bookId, chapter.id, chapter.title, volumeTitle, index, target.absolutePath, target.length())
                dao.upsertChapter(row); existing[chapter.id] = row
                dao.upsertDownload((dao.get(task.ownerId, task.bookId) ?: current).copy(
                    status = OfflineDownloadStatus.DOWNLOADING.name, totalChapters = items.size,
                    completedChapters = existing.size, downloadedBytes = existing.values.sumOf { it.byteSize }, updatedAt = System.currentTimeMillis(),
                ))
            }
            val latest = dao.get(task.ownerId, task.bookId) ?: return@withContext
            if (latest.status != OfflineDownloadStatus.PAUSED.name) dao.upsertDownload(latest.copy(status = OfflineDownloadStatus.COMPLETED.name, completedChapters = items.size, errorMessage = null, updatedAt = System.currentTimeMillis()))
            // 新增占用已落定，此时才把目录压回预算；本书正在被阅读，予以排除。
            enforceBudget(excludeBookId = task.bookId)
        } catch (error: Exception) {
            val latest = dao.get(task.ownerId, task.bookId) ?: current
            if (latest.status != OfflineDownloadStatus.PAUSED.name) dao.upsertDownload(latest.copy(status = OfflineDownloadStatus.FAILED.name, errorMessage = error.message ?: "离线下载失败", updatedAt = System.currentTimeMillis()))
        }
    }

    internal suspend fun resumable(): List<OfflineDownloadEntity> = sessionManager.currentUserId()?.let { dao.resumable(it) }.orEmpty()

    private suspend fun updateStatus(bookId: String, status: OfflineDownloadStatus, error: String? = null) {
        val owner = requireOwner(); val rawId = canonical(bookId); val task = dao.get(owner, rawId) ?: return
        dao.upsertDownload(task.copy(status = status.name, errorMessage = error, updatedAt = System.currentTimeMillis()))
    }

    private suspend fun requireOwner() = sessionManager.currentUserId() ?: error("登录后才能管理离线内容")
    private fun canonical(value: String) = value.removePrefix("content:")
    private fun safe(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun bookDirectory(owner: String, bookId: String) = File(context.filesDir, "offline-reading/${safe(owner)}/${safe(bookId)}")
    private fun OfflineDownloadEntity.toModel() = OfflineDownloadState(bookId, title, OfflineDownloadStatus.valueOf(status), completedChapters, totalChapters, downloadedBytes, errorMessage)
}
