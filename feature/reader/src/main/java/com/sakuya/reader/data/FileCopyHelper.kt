package com.sakuya.reader.data

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class FileCopyHelper @Inject constructor() {
    fun copyToPrivate(contentResolver: ContentResolver, uri: Uri, targetDir: File): File? {
        targetDir.mkdirs()
        val fileName = uri.lastPathSegment ?: "unknown"
        val targetFile = File(targetDir, fileName)

        targetFile.delete()

        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (e: Exception) {
            null
        }
    }
}
