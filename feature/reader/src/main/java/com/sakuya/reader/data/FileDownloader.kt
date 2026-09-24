package com.sakuya.reader.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import java.io.File
import java.util.UUID
import javax.inject.Inject

/** 导入用的源文件可能在服务端被替换，缓存超过一天即重新拉取。 */
internal const val DOWNLOAD_CACHE_TTL_MILLIS = 24L * 60L * 60L * 1000L

/** 下载前预检即超限：与网络失败区分开，让阅读页能给出准确提示。 */
class DownloadTooLargeException(val maxBytes: Long) :
    IllegalStateException("文件超过 ${maxBytes / 1024 / 1024} MiB 上限")

/** 缓存文件复用判定：缺失或已过 TTL 都必须重新下载。 */
internal fun isDownloadCacheReusable(
    lastModified: Long,
    now: Long,
    ttlMillis: Long = DOWNLOAD_CACHE_TTL_MILLIS,
): Boolean {
    // 时间戳不可用时无法判断新鲜度，按不可复用以求正确。
    if (lastModified <= 0L) return false
    // 差值可能为负（时钟回拨），此时按新鲜处理，避免时间戳超前的文件被反复下载。
    return now - lastModified < ttlMillis
}

/**
 * 尺寸预检。contentLength 为 -1 表示响应未声明长度（分块传输），
 * 此时不做判断，交由 ReaderRepository 落盘后的兜底校验负责。
 */
internal fun exceedsMaxBytes(contentLength: Long, maxBytes: Long): Boolean =
    contentLength > 0L && contentLength > maxBytes

/**
 * FileDownloader.kt
 * 职责说明：把导入用的 http/https 书籍文件下载到应用缓存目录，命中时按 TTL 复用。
 * 执行流程：按 URL 派生稳定文件名 -> 未过期直接复用，否则下载到临时文件后原子替换。
 * 原子替换保证下载中断不会留下半截文件被后续误判为有效缓存。
 */
class FileDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val retrofit: Retrofit
) {

    suspend fun downloadToCache(url: String, maxBytes: Long): File = withContext(Dispatchers.IO) {

        val extension = url.substringBefore('?').substringAfterLast('.', "bin")
        val fileName = "${UUID.nameUUIDFromBytes(url.toByteArray())}.$extension"
        val file = File(context.cacheDir, fileName)

        // 文件名只由 URL 派生，若不设 TTL，同一 URL 的内容更新后将永远读到旧文件。
        if (isDownloadCacheReusable(file.lastModified(), System.currentTimeMillis())) return@withContext file

        val resolvedUrl = retrofit.baseUrl().resolve(url)
            ?: error("无效的书籍下载地址")
        val request = Request.Builder().url(resolvedUrl).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("下载书籍失败（HTTP ${response.code}）")
            }
            val body = response.body ?: error("服务器未返回书籍内容")
            // 先按声明长度预检，超限时不落盘，避免把超大正文完整写入磁盘。
            if (exceedsMaxBytes(body.contentLength(), maxBytes)) throw DownloadTooLargeException(maxBytes)
            val temporary = File(file.parentFile, file.name + ".part")
            try {
                body.byteStream().use { input ->
                    temporary.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (!temporary.renameTo(file)) {
                    temporary.copyTo(file, overwrite = true)
                    temporary.delete()
                }
            } catch (error: Throwable) {
                temporary.delete()
                throw error
            }
        }
        file
    }
}
