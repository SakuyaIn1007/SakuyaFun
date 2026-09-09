package com.sakuya.library.data.repository

import com.sakuya.data.catalog.CatalogBook
import com.sakuya.data.local.dao.BookmarkDao
import com.sakuya.data.local.dao.LibraryBookDao
import com.sakuya.data.local.dao.ReadingProgressDao
import com.sakuya.data.local.entity.LibraryBookEntity
import com.sakuya.library.model.LibraryItem
import com.sakuya.library.model.LibraryItemType
import com.sakuya.library.model.LibrarySyncStatus
import com.sakuya.library.data.remote.LibraryApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.sakuya.data.reading.OnlineReadingStateRepository

@Singleton
/**
 * LibraryRepository.kt
 * 职责说明：协调云端书架、本地导入书籍及两类阅读进度，向书架页面提供统一列表。
 * 执行流程：书架元数据从 API 写入 Room；本地书使用旧进度表，在线书按当前 ownerId 读取云同步缓存。
 */
class LibraryRepository @Inject constructor(
    private val libraryBookDao: LibraryBookDao,
    private val readingProgressDao: ReadingProgressDao,
    private val bookmarkDao: BookmarkDao,
    private val apiService: LibraryApiService,
    private val onlineReading: OnlineReadingStateRepository,
) {
    private val _syncStatus = MutableStateFlow(LibrarySyncStatus.SYNCED)
    val syncStatus = _syncStatus.asStateFlow()

    /** 同步状态独立于列表数据，以便书架在保留本地内容时显示同步中的反馈。 */
    suspend fun syncLibrary() {
        _syncStatus.value = LibrarySyncStatus.SYNCING
        runCatching {
            val response = apiService.getLibrary()
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.isSuccess()) error(body?.message ?: "同步书架失败（HTTP ${response.code()}）")
            val remoteItems = body.data ?: error("服务器未返回书架数据")
            val now = System.currentTimeMillis()
            remoteItems.forEach { item ->
                val existing = libraryBookDao.getBook(item.id)
                libraryBookDao.upsertBook(LibraryBookEntity(item.id, item.title, item.subtitle, item.filePath, item.rating, item.tags.joinToString(","), item.type.name, existing?.collectedAt ?: now, now))
            }
        }.onSuccess { _syncStatus.value = LibrarySyncStatus.SYNCED }
            .onFailure { _syncStatus.value = LibrarySyncStatus.FAILED; throw it }
    }
    fun observeBooks(): Flow<List<LibraryItem>> {
        return combine(
            libraryBookDao.observeBooks(),
            readingProgressDao.observeProgress(),
            onlineReading.observeProgresses(),
        ){books, progressList, onlineProgress ->
            val progressMap = progressList.associateBy { it.bookKey }
            val onlineMap = onlineProgress.associateBy { it.bookId }
            books.map { book->
                val remote = onlineMap[book.id]
                book.toLibraryItem(
                    progress = remote?.progress ?: progressMap[book.id]?.progress ?: 0f,
                    lastReadAt = remote?.modifiedAt?.formatTime() ?: progressMap[book.id]?.updatedAt?.formatTime(),
                )

            }
        }
    }

    suspend fun importBook(
        filePath: String,
        title: String,
        type: LibraryItemType
    ) {
        val displayTitle = title
            .removeSuffix(".txt")
            .removeSuffix(".TXT")
            .removeSuffix(".epub")
            .removeSuffix(".EPUB")
            .ifBlank { "未命名书籍" }
        val existing = libraryBookDao.getBookByTitle(displayTitle)
        val bookId = existing?.id ?: generateBookId(filePath)
        val now = System.currentTimeMillis()
        libraryBookDao.upsertBook(
            LibraryBookEntity(
                id = bookId,
                filePath = filePath,
                title = existing?.title ?: displayTitle,
                subtitle = existing?.subtitle ?: when (type) {
                    LibraryItemType.TXT -> "本地 TXT"
                    LibraryItemType.EPUB -> "本地 EPUB"
                },
                rating = existing?.rating ?: 0f,
                tags = existing?.tags ?: "本地导入",
                type = type.name,
                collectedAt = existing?.collectedAt ?: now,
                updatedAt = now
            )
        )
    }

    suspend fun addCatalogBook(book: CatalogBook) {
        val response = apiService.addBook(book.id)
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.isSuccess()) {
            error(body?.message ?: "加入云端书架失败（HTTP ${response.code()}）")
        }
        val remoteItem = body.data ?: error("服务器未返回书架数据")
        val existing = libraryBookDao.getBook(book.id)
        val now = System.currentTimeMillis()
        libraryBookDao.upsertBook(
            LibraryBookEntity(
                id = book.id,
                filePath = remoteItem.filePath.ifBlank { existing?.filePath.orEmpty() },
                title = remoteItem.title,
                subtitle = remoteItem.subtitle,
                rating = remoteItem.rating,
                tags = remoteItem.tags.joinToString(","),
                type = remoteItem.type.name,
                collectedAt = existing?.collectedAt ?: now,
                updatedAt = now
            )
        )
    }

    fun observeRecentBooks(): Flow<List<LibraryItem>> {
        return combine(
            libraryBookDao.observeBooks(),
            readingProgressDao.observeRecentProgress(),
            onlineReading.observeProgresses(),
        ){ books, progressList, onlineProgress ->
            val bookMap = books.associateBy { it.id }
            val local = progressList.mapNotNull { progress ->
                bookMap[progress.bookKey]?.let { book -> Triple(progress.updatedAt, book, progress.progress) }
            }
            val remote = onlineProgress.mapNotNull { progress ->
                bookMap[progress.bookId]?.let { book -> Triple(progress.modifiedAt, book, progress.progress) }
            }
            (local + remote).sortedByDescending { it.first }.distinctBy { it.second.id }
                .map { (updatedAt, book, progress) -> book.toLibraryItem(progress, updatedAt.formatTime()) }
        }
    }

    private fun generateBookId(filePath: String): String {
        return UUID.nameUUIDFromBytes(filePath.toByteArray()).toString()
    }

    suspend fun removeBook(id: String) {
        val existing = libraryBookDao.getBook(id)
        val response = apiService.removeBook(id)
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.isSuccess()) {
            error(body?.message ?: "删除云端书架项目失败（HTTP ${response.code()}）")
        }
        libraryBookDao.deleteBook(id)
        if (existing?.filePath.isNullOrBlank()) {
            onlineReading.deleteBook(id)
        } else {
            readingProgressDao.deleteProgress(id)
            bookmarkDao.deleteBookmarkByBookId(id)
        }
    }

    private fun LibraryBookEntity.toLibraryItem(progress: Float, lastReadAt: String? = null): LibraryItem {
        return LibraryItem(
            id = id,
            title = title,
            subtitle = subtitle,
            rating = rating,
            tags = tags.split(",").filter { it.isNotBlank() },
            type = runCatching { LibraryItemType.valueOf(type) }
                .getOrDefault(LibraryItemType.TXT),
            collectedAt = collectedAt.formatTime(),
            filePath = filePath,
            progress = progress,
            lastReadAt = lastReadAt,
            syncStatus = if (filePath.isBlank()) LibrarySyncStatus.SYNCED else LibrarySyncStatus.LOCAL_ONLY,
        )
    }

    private fun Long.formatTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(this))
    }
}
