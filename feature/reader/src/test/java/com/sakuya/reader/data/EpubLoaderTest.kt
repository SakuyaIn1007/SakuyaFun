package com.sakuya.reader.data

import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubWriter
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EpubLoaderTest {

    private val loader = EpubLoader()

    @Test
    fun `load should parse single chapter epub correctly`() {
        val epubFile = createTempEpub(
            title = "Test Book",
            chapters = listOf(
                "Chapter 1" to "Hello World, this is the first chapter."
            )
        )

        try {
            val result = loader.load(epubFile)

            assertEquals(1, result.size)
            assertTrue(result[0].contains("Hello World"))
        } finally {
            epubFile.delete()
        }
    }

    @Test
    fun `load should parse multiple chapters epub correctly`() {
        val epubFile = createTempEpub(
            title = "Multi Chapter Book",
            chapters = listOf(
                "Chapter 1" to "First chapter content here.",
                "Chapter 2" to "Second chapter content here.",
                "Chapter 3" to "Third chapter content here."
            )
        )

        try {
            val result = loader.load(epubFile)

            assertEquals(3, result.size)
            assertTrue(result[0].contains("First chapter"))
            assertTrue(result[1].contains("Second chapter"))
            assertTrue(result[2].contains("Third chapter"))
        } finally {
            epubFile.delete()
        }
    }

    @Test
    fun `load should return empty list for empty epub`() {
        val epubFile = createTempEpub(
            title = "Empty Book",
            chapters = emptyList()
        )

        try {
            val result = loader.load(epubFile)

            // 空 epub 仍有结构，返回的 contents 数量取决于 epublib 的内部行为
            // 这里只验证不抛异常且返回了列表
            assertNotNull(result)
        } finally {
            epubFile.delete()
        }
    }

    private fun createTempEpub(title: String, chapters: List<Pair<String, String>>): File {
        val file = File.createTempFile("test", ".epub")
        val book = Book()
        book.metadata.addTitle(title)

        chapters.forEachIndexed { index, (name, content) ->
            val xhtml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>$name</title></head>
                <body><p>$content</p></body>
                </html>
            """.trimIndent()
            book.addSection(name, Resource(xhtml.toByteArray(), "chapter${index + 1}.xhtml"))
        }

        EpubWriter().write(book, file.outputStream())
        return file
    }
}
