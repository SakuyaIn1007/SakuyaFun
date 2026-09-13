package com.sakuya.reader.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sakuya.data.local.dao.BookmarkDao
import com.sakuya.data.local.dao.ReadingProgressDao
import com.sakuya.data.local.entity.BookmarkEntity
import com.sakuya.data.local.entity.ReadingProgressEntity
import com.sakuya.data.content.ContentApiService
import com.sakuya.data.reading.OnlineReadingBookmark
import com.sakuya.data.reading.OnlineReadingPosition
import com.sakuya.data.reading.OnlineReadingStateRepository
import com.sakuya.data.offline.OfflineDownloadState
import com.sakuya.data.offline.OfflineReadingRepository
import com.sakuya.reader.data.EpubLoader
import com.sakuya.reader.data.FileDownloader
import com.sakuya.reader.data.TxtLoader
import com.sakuya.reader.data.mapReadableAnchors
import com.sakuya.reader.model.ReaderChapter
import com.sakuya.reader.model.ReaderDocument
import com.sakuya.reader.model.ReaderBookmark
import com.sakuya.reader.model.ReaderOpenResult
import com.sakuya.reader.model.ReaderParseError
import com.sakuya.reader.model.ReaderReadingPosition
import com.sakuya.reader.model.ReaderType
import com.sakuya.reader.model.Wenku8NovelOpenResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.net.SocketTimeoutException
import java.util.UUID
import android.content.Intent
import androidx.core.content.ContextCompat
import com.sakuya.reader.tts.ReaderTtsService
import com.sakuya.reader.tts.ReaderTtsPlayback
import javax.inject.Inject

/**
 * ReaderRepository.kt
 * 职责说明：统一本地 TXT/EPUB 与在线内容库的打开、进度和书签数据访问。
 * 执行流程：本地文件继续写原 Room 表；content:* 在线书委托 OnlineReadingStateRepository，
 * 由其先离线落库再自动云同步，格式解析和同步细节均不暴露给 ViewModel。
 */
class ReaderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val epubLoader: EpubLoader,
    private val txtLoader: TxtLoader,
    private val fileDownloader: FileDownloader,
    private val contentApi: ContentApiService,
    private val readingProgressDao: ReadingProgressDao,
    private val bookmarkDao: BookmarkDao,
    private val onlineReading: OnlineReadingStateRepository,
    private val offlineReading: OfflineReadingRepository,
) {

    val syncStatus = onlineReading.syncStatus
    val syncError = onlineReading.syncError
    val ttsPlayback = ReaderTtsPlayback.state

    /** 远端章节统一从后端内容库读取；读取成功后仅生成内存 TXT 文档交给既有阅读 UI。 */
    suspend fun openRemoteChapter(novelId: String, chapterId: String, fallbackTitle: String): ReaderOpenResult = withContext(Dispatchers.IO) {
        runCatching {
            val response = contentApi.chapter(chapterId, novelId)
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.isSuccess()) error(body?.message ?: "章节加载失败")
            val data = body.data ?: error("服务器未返回章节正文")
            val content = data.content.orEmpty()
            if (content.isBlank()) return@withContext ReaderOpenResult.Failure(ReaderParseError.EmptyDocument)
            // 内容兼容接口可能不返回标题，导航层传入的目录标题是可靠回退值。
            ReaderOpenResult.Success(ReaderDocument.Txt(data.title.orEmpty().ifBlank { fallbackTitle }, content))
        }.getOrElse { error -> ReaderOpenResult.Failure(error.toReaderParseError()) }
    }

    /**
     * 连续阅读执行流程：先请求全文与章节锚点 -> 锚点可用则建立会话内全文文档 ->
     * 任意上游限制、正文异常或目标章无法定位时，再按原接口读取当前章节作为可恢复降级。
     */
    suspend fun openRemoteNovel(novelId: String, targetChapterId: String?, fallbackTitle: String): Wenku8NovelOpenResult = withContext(Dispatchers.IO) {
        runCatching {
            val response = contentApi.fullContent(novelId)
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.isSuccess()) error(body?.message ?: "连续阅读加载失败")
            val data = body.data ?: error("服务器未返回全文内容")
            val content = data.content.orEmpty()
            if (content.isBlank()) error("连续阅读正文为空")
            val anchors = mapReadableAnchors(data.chapters, content.length)
            // targetChapterId 为 null 表示「整本阅读」：从头开始，无需在锚点中定位目标章。
            if (targetChapterId != null && anchors.none { it.chapterId == targetChapterId }) error("当前章节无法在全文中定位")
            Wenku8NovelOpenResult.Full(
                ReaderDocument.Wenku8Full(
                    title = data.title.orEmpty().ifBlank { fallbackTitle },
                    text = content,
                    chapters = anchors
                )
            )
        }.getOrElse { fullError ->
            val offline = offlineReading.readChapters(novelId)
            if (offline.isNotEmpty()) {
                val builder = StringBuilder()
                val anchors = offline.map { chapter ->
                    if (builder.isNotEmpty()) builder.append("\n\n")
                    val anchor = com.sakuya.reader.model.Wenku8ChapterAnchor(chapter.chapterId, chapter.title, chapter.volumeTitle, builder.length)
                    builder.append(chapter.title).append("\n\n").append(chapter.content)
                    anchor
                }
                return@withContext Wenku8NovelOpenResult.Full(
                    ReaderDocument.Wenku8Full(fallbackTitle, builder.toString(), anchors)
                )
            }
            // 整本阅读没有目标章可降级；全文不可用时只能如实报告失败。
            if (targetChapterId == null) return@withContext Wenku8NovelOpenResult.Failure(
                ReaderParseError.InvalidContent("连续阅读加载失败：${fullError.message}")
            )
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
        if (documentSizeBytes(localUri)?.let { it > MAX_DOCUMENT_BYTES } == true) {
            return@withContext ReaderOpenResult.Failure(ReaderParseError.FileTooLarge)
        }
        if (localUri.scheme != "content" && !File(localUri.path.orEmpty()).isFile) {
            return@withContext ReaderOpenResult.Failure(ReaderParseError.FileNotFound)
        }
        val extension = resolveExtension(localUri)
        when (extension) {
            "txt" -> openTxt(localUri)?.let(ReaderOpenResult::Success) ?: ReaderOpenResult.Failure(ReaderParseError.InvalidContent())
            "epub" -> openEpub(localUri)?.let(ReaderOpenResult::Success) ?: ReaderOpenResult.Failure(ReaderParseError.InvalidContent())
            null -> ReaderOpenResult.Failure(ReaderParseError.UnsupportedFormat)
            else -> ReaderOpenResult.Failure(ReaderParseError.UnsupportedFormat)
        }
    }

    fun observeBookmarks(bookId: String): Flow<List<ReaderBookmark>> = if (bookId.isOnlineBookKey()) {
        onlineReading.observeBookmarks(bookId).map { bookmarks -> bookmarks.map { it.toReaderBookmark() } }
    } else {
        bookmarkDao.observeBookmarks(bookId).map { bookmarks -> bookmarks.map { bookmark ->
            ReaderBookmark(
                id = bookmark.id,
                title = bookmark.title,
                progress = bookmark.progress,
                note = bookmark.note,
                createdAt = bookmark.createdAt,
                chapterId = bookmark.chapterId,
                chapterProgress = bookmark.chapterProgress,
            )
        } }
    }

    /** 书签保存当前稳定位置，章节信息缺失时仍可按全局比例恢复。 */
    suspend fun addBookmark(bookId: String, position: ReaderReadingPosition, title: String) {
        val safePosition = position.normalized
        if (bookId.isOnlineBookKey()) {
            onlineReading.addBookmark(
                OnlineReadingBookmark(
                    id = UUID.randomUUID().toString(),
                    bookId = bookId,
                    title = title.ifBlank { "进度 ${(safePosition.progress * 100).toInt()}%" },
                    progress = safePosition.progress,
                    note = "",
                    createdAt = System.currentTimeMillis(),
                    chapterId = safePosition.chapterId,
                    chapterProgress = safePosition.chapterProgress,
                )
            )
            return
        }
        bookmarkDao.upsertBookmark(
            BookmarkEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                title = title.ifBlank { "进度 ${(safePosition.progress * 100).toInt()}%" },
                progress = safePosition.progress,
                note = "",
                createdAt = System.currentTimeMillis(),
                chapterId = safePosition.chapterId,
                chapterProgress = safePosition.chapterProgress,
            )
        )
    }
    suspend fun deleteBookmark(bookKey: String, id: String) {
        if (bookKey.isOnlineBookKey()) onlineReading.deleteBookmark(id) else bookmarkDao.deleteBookmark(id)
    }


    /** 将旧 progress 记录映射为默认章节位置，保持升级前阅读历史可继续使用。 */
    suspend fun getReadingPosition(bookKey: String): ReaderReadingPosition = withContext(Dispatchers.IO) {
        if (bookKey.isOnlineBookKey()) {
            return@withContext onlineReading.getPosition(bookKey)?.let { position ->
                ReaderReadingPosition(
                    type = runCatching { ReaderType.valueOf(position.contentType) }.getOrDefault(ReaderType.WENKU8),
                    progress = position.progress,
                    chapterId = position.chapterId,
                    chapterIndex = position.chapterIndex,
                    chapterProgress = position.chapterProgress,
                ).normalized
            } ?: ReaderReadingPosition(type = ReaderType.WENKU8)
        }
        readingProgressDao.getProgress(bookKey)?.let { entity ->
            ReaderReadingPosition(
                type = runCatching { ReaderType.valueOf(entity.contentType) }.getOrDefault(ReaderType.TXT),
                progress = entity.progress,
                chapterId = entity.chapterId,
                chapterIndex = entity.chapterIndex,
                chapterProgress = entity.chapterProgress,
            ).normalized
        } ?: ReaderReadingPosition()
    }

    /** 所有正文渲染器统一经此入口写入进度，避免格式差异泄漏到 DAO 层。 */
    suspend fun saveReadingPosition(bookKey: String, position: ReaderReadingPosition) = withContext(Dispatchers.IO) {
        val safePosition = position.normalized
        if (bookKey.isOnlineBookKey()) {
            onlineReading.savePosition(
                OnlineReadingPosition(
                    bookId = bookKey,
                    contentType = safePosition.type.name,
                    progress = safePosition.progress,
                    chapterId = safePosition.chapterId,
                    chapterIndex = safePosition.chapterIndex,
                    chapterProgress = safePosition.chapterProgress,
                    modifiedAt = System.currentTimeMillis(),
                )
            )
            return@withContext
        }
        readingProgressDao.upsertProgress(
            ReadingProgressEntity(
                bookKey = bookKey,
                progress = safePosition.progress,
                updatedAt = System.currentTimeMillis(),
                contentType = safePosition.type.name,
                chapterId = safePosition.chapterId,
                chapterIndex = safePosition.chapterIndex,
                chapterProgress = safePosition.chapterProgress,
            )
        )
    }

    suspend fun deleteProgress(bookKey: String) = withContext(Dispatchers.IO) {
        if (bookKey.isOnlineBookKey()) onlineReading.deleteBook(bookKey) else readingProgressDao.deleteProgress(bookKey)
    }

    /**
     * 首次打开统一内容时迁移旧版 wenku8:* 进度与书签。
     * 新键已存在时不覆盖，避免旧记录回写覆盖用户升级后的最新位置。
     */
    suspend fun migrateLegacyRemoteState(bookId: String): String = withContext(Dispatchers.IO) {
        val canonicalKey = "content:$bookId"
        onlineReading.migrateLegacy(bookId)
        canonicalKey
    }

    suspend fun syncOnlineState(): Result<Unit> = onlineReading.syncNow()

    suspend fun observeOfflineDownload(bookId: String): Flow<OfflineDownloadState?> = offlineReading.observe(bookId)
    suspend fun enqueueOfflineDownload(bookId: String, title: String) = offlineReading.enqueue(bookId, title)
    suspend fun pauseOfflineDownload(bookId: String) = offlineReading.pause(bookId)
    suspend fun resumeOfflineDownload(bookId: String) = offlineReading.resume(bookId)
    suspend fun retryOfflineDownload(bookId: String) = offlineReading.retry(bookId)
    suspend fun deleteOfflineDownload(bookId: String) = offlineReading.delete(bookId)
    suspend fun offlineStorageBytes(): Long = offlineReading.totalBytes()

    /** 正文先写应用缓存文件，避免把大文本放进 Intent 触发 Binder 大小限制。 */
    suspend fun startTts(title: String, text: String, progress: Float, rate: Float) = withContext(Dispatchers.IO) {
        val start = (text.length * progress.coerceIn(0f, 1f)).toInt().coerceIn(0, text.length)
        val file = File(context.cacheDir, "reader-tts.txt").apply { writeText(text.substring(start)) }
        ContextCompat.startForegroundService(context, Intent(context, ReaderTtsService::class.java).apply {
            action = ReaderTtsService.ACTION_START
            putExtra(ReaderTtsService.EXTRA_FILE, file.absolutePath)
            putExtra(ReaderTtsService.EXTRA_TITLE, title)
            putExtra(ReaderTtsService.EXTRA_RATE, rate)
        })
    }
    fun pauseTts() = context.startService(Intent(context, ReaderTtsService::class.java).setAction(ReaderTtsService.ACTION_PAUSE))
    fun resumeTts() = context.startService(Intent(context, ReaderTtsService::class.java).setAction(ReaderTtsService.ACTION_RESUME))
    fun stopTts() = context.startService(Intent(context, ReaderTtsService::class.java).setAction(ReaderTtsService.ACTION_STOP))

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
            chapters = chapters.mapIndexed { index, chapter ->
                ReaderChapter(
                    index = index,
                    title = chapter.title,
                    content = chapter.content
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

    /**
     * 导入前优先读取可用文件大小，避免超大正文在内存中完整展开造成 OOM。
     * content URI 若供应商未提供大小则继续由解析器处理，不把未知大小误判为失败。
     */
    private fun documentSizeBytes(uri: Uri): Long? = runCatching {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
            }
        } else {
            File(uri.path.orEmpty()).takeIf { it.isFile }?.length()
        }
    }.getOrNull()

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

    private fun Throwable.toReaderParseError(): ReaderParseError = when (this) {
        is SocketTimeoutException -> ReaderParseError.RemoteTimeout
        else -> ReaderParseError.InvalidContent(message)
    }

    private companion object {
        /** 15 MiB 足以覆盖普通 TXT/EPUB，同时防止导入异常大文件占满应用内存。 */
        const val MAX_DOCUMENT_BYTES = 15L * 1024L * 1024L
    }
}

private fun String.isOnlineBookKey(): Boolean = startsWith("content:")

private fun OnlineReadingBookmark.toReaderBookmark() = ReaderBookmark(
    id = id,
    title = title,
    progress = progress,
    note = note,
    createdAt = createdAt,
    chapterId = chapterId,
    chapterProgress = chapterProgress,
)
