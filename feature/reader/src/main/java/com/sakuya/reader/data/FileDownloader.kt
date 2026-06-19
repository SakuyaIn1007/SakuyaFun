package com.sakuya.reader.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject

class FileDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun downloadToCache(url: String): File = withContext(Dispatchers.IO) {

        val fileName = url.substringAfterLast("/")
        val file = File(context.cacheDir, fileName)

        if (file.exists()) return@withContext file

        val connection = URL(url).openConnection()
        connection.connect()

        connection.getInputStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file
    }
}