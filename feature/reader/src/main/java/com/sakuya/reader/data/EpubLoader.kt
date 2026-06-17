package com.sakuya.reader.data

import nl.siegmann.epublib.epub.EpubReader
import java.io.File

open class EpubLoader {
    open fun load(file: File): List<String> {
        val book = EpubReader().readEpub(file.inputStream())

        return book.contents.map { resource ->
            resource.reader.readText()
        }
    }
}