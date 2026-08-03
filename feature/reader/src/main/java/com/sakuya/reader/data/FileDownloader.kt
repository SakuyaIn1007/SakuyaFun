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

class FileDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val retrofit: Retrofit
) {

    suspend fun downloadToCache(url: String): File = withContext(Dispatchers.IO) {

        val extension = url.substringBefore('?').substringAfterLast('.', "bin")
        val fileName = "${UUID.nameUUIDFromBytes(url.toByteArray())}.$extension"
        val file = File(context.cacheDir, fileName)

        if (file.exists()) return@withContext file

        val resolvedUrl = retrofit.baseUrl().resolve(url)
            ?: error("无效的书籍下载地址")
        val request = Request.Builder().url(resolvedUrl).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("下载书籍失败（HTTP ${response.code}）")
            }
            val body = response.body ?: error("服务器未返回书籍内容")
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        file
    }
}
