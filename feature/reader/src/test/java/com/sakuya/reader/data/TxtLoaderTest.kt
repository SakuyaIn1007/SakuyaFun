package com.sakuya.reader.data

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TxtLoaderTest {

    private val loader = TxtLoader()

    @Test
    fun `load should return full text content`() {
        val file = createTempTxt(listOf("Line 1", "Line 2", "Line 3"))
        try {
            val result = loader.load(file.inputStream())
            assertEquals("Line 1\nLine 2\nLine 3", result)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `load should handle empty file`() {
        val file = File.createTempFile("empty", ".txt")
        try {
            val result = loader.load(file.inputStream())
            assertEquals("", result)
        } finally {
            file.delete()
        }
    }

    private fun createTempTxt(lines: List<String>): File {
        val file = File.createTempFile("test", ".txt")
        file.writeText(lines.joinToString("\n"))
        return file
    }
}
