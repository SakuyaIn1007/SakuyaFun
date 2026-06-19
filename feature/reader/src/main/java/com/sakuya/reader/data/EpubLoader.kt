package com.sakuya.reader.data

import nl.siegmann.epublib.epub.EpubReader
import java.io.InputStream
import javax.inject.Inject

open class EpubLoader @Inject constructor() {
    fun load(inputStream: InputStream): List<String> {
        val book = EpubReader().readEpub(inputStream)
        val spineResources = book.spine.spineReferences
            .mapNotNull { it.resource }
            .ifEmpty {
                book.contents.filter { resource ->
                    resource.mediaType?.name in setOf(
                        "application/xhtml+xml",
                        "text/html"
                    )
                }
            }

        return spineResources.mapNotNull { resource ->
            runCatching {
                resource.reader.readText()
            }.getOrNull()?.takeIf { it.isNotBlank() }
        }
    }
}
