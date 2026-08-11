package com.sakuya.reader.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sakuya.data.local.dao.BookmarkDao
import com.sakuya.data.local.dao.ReadingProgressDao
import com.sakuya.data.local.entity.BookmarkEntity
import com.sakuya.data.local.entity.ReadingProgressEntity
import com.sakuya.reader.data.EpubLoader
import com.sakuya.reader.data.FileDownloader
import com.sakuya.reader.data.TxtLoader
import com.sakuya.reader.model.ReaderChapter
import com.sakuya.reader.model.ReaderDocument
import com.sakuya.reader.model.ReaderBookmark
import com.sakuya.reader.model.ReaderOpenResult
import com.sakuya.reader.model.ReaderParseError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

//ReaderRepository处理文件获取
class ReaderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val epubLoader: EpubLoader,
    private val txtLoader: TxtLoader,
    private val fileDownloader: FileDownloader,
    private val readingProgressDao: ReadingProgressDao,
    private val bookmarkDao: BookmarkDao
) {

    /** 将文件下载、格式识别和解析失败细分为领域错误，避免阅读页只能显示模糊的空结果。 */
    suspend fun openDocument(uri: Uri): ReaderOpenResult = withContext(Dispatchers.IO) {
        val localUri = if (uri.scheme == "http" || uri.scheme == "https") {
            runCatching { Uri.fromFile(fileDownloader.downloadToCache(uri.toString())) }.getOrNull()
                ?: return@withContext ReaderOpenResult.Failure(ReaderParseError.DownloadFailed)
        } else {
            uri
        }
        val extension = resolveExtension(localUri)
        when (extension) {
            "txt" -> openTxt(localUri)?.let(ReaderOpenResult::Success) ?: ReaderOpenResult.Failure(ReaderParseError.InvalidContent())
            "epub" -> openEpub(localUri)?.let(ReaderOpenResult::Success) ?: ReaderOpenResult.Failure(ReaderParseError.InvalidContent())
            null -> ReaderOpenResult.Failure(ReaderParseError.UnsupportedFormat)
            else -> ReaderOpenResult.Failure(ReaderParseError.UnsupportedFormat)
        }
    }

    fun observeBookmarks(bookId: String): Flow<List<ReaderBookmark>> = bookmarkDao.observeBookmarks(bookId).map { bookmarks ->
        bookmarks.map { bookmark -> ReaderBookmark(bookmark.id, bookmark.title, bookmark.progress, bookmark.note, bookmark.createdAt) }
    }

    suspend fun addBookmark(bookId: String, progress: Float) {
        val safeProgress = progress.coerceIn(0f, 1f)
        bookmarkDao.upsertBookmark(
            BookmarkEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                title = "进度 ${(safeProgress * 100).toInt()}%",
                progress = safeProgress,
                note = "",
                createdAt = System.currentTimeMillis()
            )
        )
    }
    suspend fun deleteBookmark(id: String) {
        bookmarkDao.deleteBookmark(id)
    }


    suspend fun getProgress(bookKey: String): Float = withContext(Dispatchers.IO) {
        readingProgressDao.getProgress(bookKey)?.progress ?: 0f
    }

    suspend fun saveProgress(bookKey: String, progress: Float) = withContext(Dispatchers.IO) {
        readingProgressDao.upsertProgress(
            ReadingProgressEntity(
                bookKey = bookKey,
                progress = progress.coerceIn(0f, 1f),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteProgress(bookKey: String) = withContext(Dispatchers.IO) {
        readingProgressDao.deleteProgress(bookKey)
    }

    private fun openTxt(uri: Uri): ReaderDocument.Txt? {
        val inputStream = openInputStream(uri) ?: return null
        val text = runCatching {
            inputStream.use { txtLoader.load(it) }
        }.getOrNull() ?: return null

        return ReaderDocument.Txt(
            title = resolveTitle(uri, fallback = "TXT"),
            text = text
        )
    }

    private fun openEpub(uri: Uri): ReaderDocument.Epub? {
        val inputStream = openInputStream(uri) ?: return null
        val chapters = runCatching {
            inputStream.use { epubLoader.load(it) }
        }.getOrNull() ?: return null
        if (chapters.isEmpty()) return null

        return ReaderDocument.Epub(
            title = resolveTitle(uri, fallback = "EPUB"),
            chapters = chapters.mapIndexed { index, content ->
                ReaderChapter(
                    index = index,
                    title = "第${index + 1}章",
                    content = content
                )
            }
        )
    }

    private fun openInputStream(uri: Uri) =
        try {
            if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)
            } else {
                File(uri.path.orEmpty()).inputStream()
            }
        } catch (e: Exception) {
            null
        }

    private fun resolveExtension(uri: Uri): String? {
        val mimeExtension = if (uri.scheme == "content") {
            when (safeMimeType(uri)) {
                "text/plain" -> "txt"
                "application/epub+zip" -> "epub"
                else -> null
            }
        } else {
            null
        }

        return mimeExtension
            ?: resolveTitle(uri, fallback = "")
                .substringAfterLast(".", "")
                .lowercase()
                .takeIf { it == "txt" || it == "epub" }
            ?: uri.lastPathSegment
                ?.substringAfterLast(".", "")
                ?.lowercase()
                ?.takeIf { it == "txt" || it == "epub" }
    }

    private fun safeMimeType(uri: Uri): String? {
        return runCatching {
            context.contentResolver.getType(uri)
        }.getOrNull()
    }

    private fun resolveTitle(uri: Uri, fallback: String): String {
        val displayName = if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        cursor.getString(nameIndex)
                    } else {
                        null
                    }
                }
            }.getOrNull()
        } else {
            null
        }

        return displayName
            ?: uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: fallback
    }
}
