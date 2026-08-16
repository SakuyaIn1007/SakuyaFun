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
import com.sakuya.reader.data.Wenku8ReaderApiService
import com.sakuya.reader.data.mapReadableAnchors
import com.sakuya.reader.model.ReaderChapter
import com.sakuya.reader.model.ReaderDocument
import com.sakuya.reader.model.ReaderBookmark
import com.sakuya.reader.model.ReaderOpenResult
import com.sakuya.reader.model.ReaderParseError
import com.sakuya.reader.model.Wenku8NovelOpenResult
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
    private val wenku8Api: Wenku8ReaderApiService,
    private val readingProgressDao: ReadingProgressDao,
    private val bookmarkDao: BookmarkDao
) {

    /** 远端章节不会落盘；读取成功后仅生成内存 TXT 文档交给既有阅读 UI。 */
    suspend fun openRemoteChapter(novelId: String, chapterId: String, fallbackTitle: String): ReaderOpenResult = withContext(Dispatchers.IO) {
        runCatching {
            val response = wenku8Api.content(chapterId, novelId)
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.isSuccess()) error(body?.message ?: "章节加载失败")
            val data = body.data ?: error("服务器未返回章节正文")
            val content = data.content.orEmpty()
            if (content.isBlank()) return@withContext ReaderOpenResult.Failure(ReaderParseError.EmptyDocument)
            // Wenku8 章节正文接口未承诺返回标题，导航层传入的目录标题是可靠回退值。
            ReaderOpenResult.Success(ReaderDocument.Txt(data.title.orEmpty().ifBlank { fallbackTitle }, content))
        }.getOrElse { ReaderOpenResult.Failure(ReaderParseError.InvalidContent(it.message)) }
    }

    /**
     * 连续阅读执行流程：先请求全文与章节锚点 -> 锚点可用则建立会话内全文文档 ->
     * 任意上游限制、正文异常或目标章无法定位时，再按原接口读取当前章节作为可恢复降级。
     */
    suspend fun openRemoteNovel(novelId: String, targetChapterId: String, fallbackTitle: String): Wenku8NovelOpenResult = withContext(Dispatchers.IO) {
        runCatching {
            val response = wenku8Api.fullContent(novelId)
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.isSuccess()) error(body?.message ?: "连续阅读加载失败")
            val data = body.data ?: error("服务器未返回全文内容")
            val content = data.content.orEmpty()
            if (content.isBlank()) error("连续阅读正文为空")
            val anchors = mapReadableAnchors(data.chapters, content.length)
            if (anchors.none { it.chapterId == targetChapterId }) error("当前章节无法在全文中定位")
            Wenku8NovelOpenResult.Full(
                ReaderDocument.Wenku8Full(
                    title = data.title.orEmpty().ifBlank { fallbackTitle },
                    text = content,
                    chapters = anchors
                )
            )
        }.getOrElse { fullError ->
            when (val chapter = openRemoteChapter(novelId, targetChapterId, fallbackTitle)) {
                is ReaderOpenResult.Success -> Wenku8NovelOpenResult.ChapterFallback(
                    document = chapter.document as? ReaderDocument.Txt
                        ?: ReaderDocument.Txt(fallbackTitle, ""),
                    notice = "连续阅读暂不可用，已切换为当前章节。"
                )
                is ReaderOpenResult.Failure -> Wenku8NovelOpenResult.Failure(
                    ReaderParseError.InvalidContent("连续阅读和当前章节均无法加载：${chapter.error.displayDetail(fullError.message)}")
                )
            }
        }
    }

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

    private fun ReaderParseError.displayDetail(fallback: String?): String = when (this) {
        is ReaderParseError.InvalidContent -> detail ?: fallback.orEmpty()
        else -> fallback.orEmpty()
    }
}
