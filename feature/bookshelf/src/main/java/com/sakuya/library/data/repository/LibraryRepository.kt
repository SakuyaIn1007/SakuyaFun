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

@Singleton
class LibraryRepository @Inject constructor(
    private val libraryBookDao: LibraryBookDao,
    private val readingProgressDao: ReadingProgressDao,
    private val bookmarkDao: BookmarkDao,
    private val apiService: LibraryApiService
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
            readingProgressDao.observeProgress()
        ){books, progressList ->
            val progressMap = progressList.associateBy { it.bookKey }
            books.map { book->
                book.toLibraryItem(
                    progress = progressMap[book.id]?.progress ?: 0f,
                    lastReadAt = progressMap[book.id]?.updatedAt?.formatTime(),
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
            readingProgressDao.observeRecentProgress()
        ){ books, progressList ->
            val bookMap = books.associateBy { it.id }

            progressList.mapNotNull { progress ->
                val book = bookMap[progress.bookKey]
                book?.toLibraryItem(progress = progress.progress, lastReadAt = progress.updatedAt.formatTime())
            }
        }
    }

    private fun generateBookId(filePath: String): String {
        return UUID.nameUUIDFromBytes(filePath.toByteArray()).toString()
    }

    suspend fun removeBook(id: String) {
        val response = apiService.removeBook(id)
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.isSuccess()) {
            error(body?.message ?: "删除云端书架项目失败（HTTP ${response.code()}）")
        }
        libraryBookDao.deleteBook(id)
        readingProgressDao.deleteProgress(id)
        bookmarkDao.deleteBookmarkByBookId(id)
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
