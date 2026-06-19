package com.sakuya.reader.navigation

import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubWriter
import java.io.File

object TestEpubGenerator {

    fun createTo(file: File) {
        val book = Book()
        book.metadata.addTitle("Test EPUB Book")
        book.metadata.addAuthor(Author("Tester", ""))

        val chapters = listOf(
            "Chapter 1" to "<h1>Chapter 1</h1><p>This is the first chapter of the test EPUB.</p>",
            "Chapter 2" to "<h1>Chapter 2</h1><p>This is the second chapter of the test EPUB.</p>",
            "Chapter 3" to "<h1>Chapter 3</h1><p>This is the third chapter of the test EPUB.</p>"
        )

        chapters.forEachIndexed { index, (title, body) ->
            val xhtml = """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>$title</title></head>
<body>$body</body>
</html>""".trimIndent()
            book.addSection(title, Resource(xhtml.toByteArray(), "chapter${index + 1}.xhtml"))
        }

        EpubWriter().write(book, file.outputStream())
    }
}
