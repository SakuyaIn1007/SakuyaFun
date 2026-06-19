package com.sakuya.reader.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sakuya.reader.data.EpubLoader
import com.sakuya.reader.data.TxtLoader
import com.sakuya.reader.model.ReaderChapter
import com.sakuya.reader.model.ReaderDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

//ReaderRepository处理文件获取
class ReaderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val epubLoader: EpubLoader,
    private val txtLoader: TxtLoader
) {
    suspend fun openDocument(uri: Uri): ReaderDocument? = withContext(Dispatchers.IO) {
        val extension = resolveExtension(uri)
        when (extension) {
            "txt" -> openTxt(uri)
            "epub" -> openEpub(uri)
            else -> openEpub(uri) ?: openTxt(uri)
        }
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
