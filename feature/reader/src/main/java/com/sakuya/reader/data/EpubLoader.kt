package com.sakuya.reader.data

import nl.siegmann.epublib.epub.EpubReader
import nl.siegmann.epublib.domain.TOCReference
import java.io.InputStream
import javax.inject.Inject

open class EpubLoader @Inject constructor() {
    fun load(inputStream: InputStream): List<EpubLoadedChapter> {
        val book = EpubReader().readEpub(inputStream)
        val tocTitles = book.tableOfContents.tocReferences
            .flatMap(::flattenToc)
            .mapNotNull { reference -> reference.resource?.href?.let { it to reference.title } }
            .toMap()
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

        return spineResources.mapIndexedNotNull { index, resource ->
            runCatching {
                val content = resource.reader.readText()
                EpubLoadedChapter(tocTitles[resource.href]?.takeIf { it.isNotBlank() } ?: resource.title?.takeIf { it.isNotBlank() } ?: "第${index + 1}章", content)
            }.getOrNull()?.takeIf { it.content.isNotBlank() }
        }
    }

    private fun flattenToc(reference: TOCReference): List<TOCReference> = listOf(reference) + reference.children.flatMap(::flattenToc)
}

data class EpubLoadedChapter(val title: String, val content: String)
