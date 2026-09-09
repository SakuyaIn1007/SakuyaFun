package com.sakuya.data.reading

import androidx.room.withTransaction
import com.sakuya.data.local.AppDatabase
import com.sakuya.data.local.SessionManager
import com.sakuya.data.local.TokenStorage
import com.sakuya.data.local.dao.BookmarkDao
import com.sakuya.data.local.dao.OnlineBookmarkDao
import com.sakuya.data.local.dao.OnlineReadingProgressDao
import com.sakuya.data.local.dao.ReadingProgressDao
import com.sakuya.data.local.entity.OnlineBookmarkEntity
import com.sakuya.data.local.entity.OnlineReadingProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OnlineReadingStateRepository.kt
 * 职责说明：作为在线阅读状态的单一数据源，统一账号隔离、Room 队列、旧数据迁移和云端快照合并。
 * 执行流程：UI 写入本地 PENDING -> Signals 唤醒协调器 -> 上传全部脏记录 -> 用服务端完整快照原子替换当前账号缓存。
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class OnlineReadingStateRepository @Inject constructor(
    private val database: AppDatabase,
    private val progressDao: OnlineReadingProgressDao,
    private val bookmarkDao: OnlineBookmarkDao,
    private val legacyProgressDao: ReadingProgressDao,
    private val legacyBookmarkDao: BookmarkDao,
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage,
    private val api: ReadingSyncApiService,
    private val signals: ReadingSyncSignals,
) {
    private val syncMutex = Mutex()
    private val _syncStatus = MutableStateFlow(ReadingSyncStatus.SYNCED)
    val syncStatus = _syncStatus.asStateFlow()
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError = _syncError.asStateFlow()

    fun observeProgresses(): Flow<List<OnlineReadingPosition>> = tokenStorage.getUserId().flatMapLatest { owner ->
        if (owner == null) flowOf(emptyList())
        else progressDao.observeActive(owner).map { rows -> rows.map { it.toModel() } }
    }

    suspend fun getPosition(bookId: String): OnlineReadingPosition? {
        val owner = sessionManager.currentUserId() ?: return null
        return progressDao.get(owner, canonicalBookId(bookId))?.takeUnless { it.deleted }?.toModel()
    }

    fun observeBookmarks(bookId: String): Flow<List<OnlineReadingBookmark>> =
        tokenStorage.getUserId().flatMapLatest { owner ->
            if (owner == null) flowOf(emptyList())
            else bookmarkDao.observeActive(owner, canonicalBookId(bookId)).map { rows -> rows.map { it.toModel() } }
        }

    suspend fun savePosition(position: OnlineReadingPosition) {
        val owner = requireOwner()
        val deviceId = sessionManager.installationId()
        progressDao.upsert(
            OnlineReadingProgressEntity(
                ownerId = owner,
                bookId = canonicalBookId(position.bookId),
                contentType = position.contentType,
                progress = position.progress.coerceIn(0f, 1f),
                chapterId = position.chapterId,
                chapterIndex = position.chapterIndex.coerceAtLeast(0),
                chapterProgress = position.chapterProgress.coerceIn(0f, 1f),
                modifiedAt = position.modifiedAt,
                deviceId = deviceId,
            )
        )
        markPending()
    }

    suspend fun addBookmark(bookmark: OnlineReadingBookmark) {
        val owner = requireOwner()
        val deviceId = sessionManager.installationId()
        bookmarkDao.upsert(bookmark.toEntity(owner, deviceId, System.currentTimeMillis()))
        markPending()
    }

    suspend fun deleteBookmark(id: String) {
        val owner = requireOwner()
        val existing = bookmarkDao.get(owner, id) ?: return
        bookmarkDao.upsert(existing.copy(deleted = true, modifiedAt = System.currentTimeMillis(), syncState = ReadingSyncStatus.PENDING.name))
        markPending()
    }

    suspend fun deleteBook(bookId: String) {
        val owner = requireOwner()
        val rawBookId = canonicalBookId(bookId)
        val now = System.currentTimeMillis()
        val deviceId = sessionManager.installationId()
        database.withTransaction {
            val existing = progressDao.get(owner, rawBookId)
            progressDao.upsert(
                existing?.copy(deleted = true, modifiedAt = now, deviceId = deviceId, syncState = ReadingSyncStatus.PENDING.name)
                    ?: OnlineReadingProgressEntity(owner, rawBookId, "WENKU8", 0f, modifiedAt = now, deviceId = deviceId, deleted = true)
            )
            bookmarkDao.getByBook(owner, rawBookId).forEach { bookmark ->
                bookmarkDao.upsert(bookmark.copy(deleted = true, modifiedAt = now, deviceId = deviceId, syncState = ReadingSyncStatus.PENDING.name))
            }
        }
        markPending()
    }

    /** 将旧 content:/wenku8: 数据只迁入首次打开该书的当前账号，迁移后删除旧副本避免重复导入。 */
    suspend fun migrateLegacy(bookId: String) {
        val owner = requireOwner()
        val rawBookId = canonicalBookId(bookId)
        val canonicalKey = "content:$rawBookId"
        val externalId = rawBookId.removePrefix("wenku8-")
        val oldKeys = listOf(canonicalKey, "wenku8:$externalId", "wenku8:$rawBookId").distinct()
        val deviceId = sessionManager.installationId()
        var migrated = false
        database.withTransaction {
            if (progressDao.get(owner, rawBookId) == null) {
                val legacy = oldKeys.firstNotNullOfOrNull { legacyProgressDao.getProgress(it) }
                if (legacy != null) {
                    progressDao.upsert(
                        OnlineReadingProgressEntity(owner, rawBookId, legacy.contentType, legacy.progress, legacy.chapterId,
                            legacy.chapterIndex, legacy.chapterProgress, System.currentTimeMillis(), deviceId)
                    )
                    migrated = true
                }
            }
            oldKeys.flatMap { legacyBookmarkDao.getBookmarks(it) }.distinctBy { it.id }.forEach { legacy ->
                if (bookmarkDao.get(owner, legacy.id) == null) {
                    bookmarkDao.upsert(
                        OnlineBookmarkEntity(owner, legacy.id, rawBookId, legacy.title, legacy.progress, legacy.note,
                            legacy.createdAt, legacy.chapterId, legacy.chapterProgress, System.currentTimeMillis(), deviceId)
                    )
                    migrated = true
                }
            }
            oldKeys.forEach { key ->
                legacyProgressDao.deleteProgress(key)
                legacyBookmarkDao.deleteBookmarkByBookId(key)
            }
        }
        if (migrated) markPending()
    }

    suspend fun syncNow(): Result<Unit> = syncMutex.withLock {
        val owner = sessionManager.currentUserId()
            ?: return@withLock Result.failure(IllegalStateException("当前登录账号不可用"))
        _syncStatus.value = ReadingSyncStatus.SYNCING
        _syncError.value = null
        var hasConcurrentWrites = false
        runCatching {
            val deviceId = sessionManager.installationId()
            val pendingProgress = progressDao.getPending(owner)
            val pendingBookmarks = bookmarkDao.getPending(owner)
            val response = api.sync(
                ReadingSyncRequestDto(
                    deviceId = deviceId,
                    progressChanges = pendingProgress.map { it.toMutation() },
                    bookmarkChanges = pendingBookmarks.map { it.toMutation() },
                )
            )
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.isSuccess()) {
                error(body?.message ?: "阅读状态同步失败（HTTP ${response.code()}）")
            }
            val snapshot = body.data ?: error("服务器未返回阅读状态")
            database.withTransaction {
                // 网络往返期间用户仍可能滚动或新增书签；只移除本次已上传的版本，保留更新的 PENDING 记录。
                val sentProgress = pendingProgress.associateBy { it.bookId }
                val sentBookmarks = pendingBookmarks.associateBy { it.id }
                val concurrentProgress = progressDao.getPending(owner).filter { current ->
                    sentProgress[current.bookId]?.modifiedAt != current.modifiedAt
                }
                val concurrentBookmarks = bookmarkDao.getPending(owner).filter { current ->
                    sentBookmarks[current.id]?.modifiedAt != current.modifiedAt
                }
                progressDao.deleteOwner(owner)
                bookmarkDao.deleteOwner(owner)
                progressDao.upsertAll(snapshot.progresses.map { it.toEntity(owner) })
                bookmarkDao.upsertAll(snapshot.bookmarks.map { it.toEntity(owner) })
                progressDao.upsertAll(concurrentProgress)
                bookmarkDao.upsertAll(concurrentBookmarks)
                hasConcurrentWrites = concurrentProgress.isNotEmpty() || concurrentBookmarks.isNotEmpty()
            }
        }.onSuccess {
            _syncStatus.value = if (hasConcurrentWrites) ReadingSyncStatus.PENDING else ReadingSyncStatus.SYNCED
            _syncError.value = null
            if (hasConcurrentWrites) signals.request()
        }.onFailure { error ->
            _syncStatus.value = ReadingSyncStatus.FAILED
            _syncError.value = error.message ?: "阅读状态同步失败"
        }
    }

    private fun markPending() {
        _syncStatus.value = ReadingSyncStatus.PENDING
        _syncError.value = null
        signals.request()
    }

    private suspend fun requireOwner(): String = sessionManager.currentUserId()
        ?: error("登录状态不可用，无法保存在线阅读状态")

    private fun canonicalBookId(bookId: String): String = bookId.removePrefix("content:")

    private fun OnlineReadingProgressEntity.toModel() = OnlineReadingPosition(
        bookId, contentType, progress, chapterId, chapterIndex, chapterProgress, modifiedAt,
    )

    private fun OnlineBookmarkEntity.toModel() = OnlineReadingBookmark(
        id, bookId, title, progress, note, createdAt, chapterId, chapterProgress,
    )

    private fun OnlineReadingBookmark.toEntity(owner: String, deviceId: String, modifiedAt: Long) = OnlineBookmarkEntity(
        owner, id, canonicalBookId(bookId), title, progress.coerceIn(0f, 1f), note, createdAt, chapterId,
        chapterProgress.coerceIn(0f, 1f), modifiedAt, deviceId,
    )

    private fun OnlineReadingProgressEntity.toMutation() = ProgressMutationDto(
        bookId, contentType, progress, chapterId, chapterIndex, chapterProgress,
        Instant.ofEpochMilli(modifiedAt).toString(), deleted,
    )

    private fun OnlineBookmarkEntity.toMutation() = BookmarkMutationDto(
        id, bookId, title, progress, note, Instant.ofEpochMilli(createdAt).toString(), chapterId, chapterProgress,
        Instant.ofEpochMilli(modifiedAt).toString(), deleted,
    )

    private fun ProgressDto.toEntity(owner: String) = OnlineReadingProgressEntity(
        owner, bookId, contentType, progress, chapterId, chapterIndex, chapterProgress,
        Instant.parse(clientModifiedAt).toEpochMilli(), deviceId, syncState = ReadingSyncStatus.SYNCED.name,
    )

    private fun BookmarkDto.toEntity(owner: String) = OnlineBookmarkEntity(
        owner, id, bookId, title, progress, note, Instant.parse(createdAt).toEpochMilli(), chapterId, chapterProgress,
        Instant.parse(clientModifiedAt).toEpochMilli(), deviceId, syncState = ReadingSyncStatus.SYNCED.name,
    )
}
