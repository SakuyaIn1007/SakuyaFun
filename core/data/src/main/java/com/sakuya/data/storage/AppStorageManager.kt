package com.sakuya.data.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppStorageManager.kt
 * 职责说明：统计并清理可安全丢弃的缓存分区，供设置页的缓存管理使用。
 * 执行流程：分区统计/清理 -> 只触碰内容 HTTP 缓存与 cacheDir 根目录的临时文件。
 *
 * 安全边界：阅读进度、书签与已下载离线内容都在 Room 与 filesDir 中，
 * 本类不引用、不删除它们，因此清理缓存不会破坏用户资产。
 * 图片缓存由 Coil 自行管理，不在此处理。
 */
@Singleton
class AppStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentHttpCache: Cache,
) {

    /** 内容接口的 HTTP 磁盘缓存占用。 */
    suspend fun contentHttpCacheBytes(): Long = withContext(Dispatchers.IO) {
        runCatching { contentHttpCache.size() }.getOrDefault(0L)
    }

    suspend fun clearContentHttpCache() = withContext(Dispatchers.IO) {
        runCatching { contentHttpCache.evictAll() }
    }

    /**
     * 仅统计 cacheDir 根目录下的文件，即 FileDownloader 的下载产物、TTS 文本等导入中间产物。
     * 不递归子目录：okhttp-content 与 image_cache 由各自的持有者管理，
     * 直接删除会导致对方持有的缓存句柄失效。
     */
    suspend fun temporaryFilesBytes(): Long = withContext(Dispatchers.IO) {
        temporaryFiles().sumOf(File::length)
    }

    suspend fun clearTemporaryFiles() = withContext(Dispatchers.IO) {
        temporaryFiles().forEach { runCatching { it.delete() } }
    }

    private fun temporaryFiles(): List<File> =
        context.cacheDir.listFiles()?.filter(File::isFile).orEmpty()
}
