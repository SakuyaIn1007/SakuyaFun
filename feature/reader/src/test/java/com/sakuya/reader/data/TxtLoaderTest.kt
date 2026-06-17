package com.sakuya.reader.data

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TxtLoaderTest {

    private val loader = TxtLoader()

    @Test
    fun `load should return correct chunks for lines divisible by 20`() {
        val file = createTempTxt(lines = (1..40).map { "Line $it" })

        try {
            val result = loader.load(file)

            assertEquals(2, result.size)
            assertTrue(result[0].startsWith("Line 1"))
            assertTrue(result[0].endsWith("Line 20"))
            assertTrue(result[1].startsWith("Line 21"))
            assertTrue(result[1].endsWith("Line 40"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `load should handle remaining lines less than chunk size`() {
        // 25 行 => 20 + 5，分两页
        val file = createTempTxt(lines = (1..25).map { "Line $it" })

        try {
            val result = loader.load(file)

            assertEquals(2, result.size)
            assertTrue(result[0].contains("Line 1"))
            assertTrue(result[0].contains("Line 20"))
            assertTrue(result[1].contains("Line 21"))
            assertTrue(result[1].contains("Line 25"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `load should return empty list for empty file`() {
        val file = File.createTempFile("empty", ".txt")

        try {
            val result = loader.load(file)

            assertTrue(result.isEmpty())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `load should handle exactly 20 lines as single chunk`() {
        val file = createTempTxt(lines = (1..20).map { "Line $it" })

        try {
            val result = loader.load(file)

            assertEquals(1, result.size)
            assertTrue(result[0].contains("Line 1"))
            assertTrue(result[0].contains("Line 20"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `load should handle single line`() {
        val file = createTempTxt(lines = listOf("Only one line"))

        try {
            val result = loader.load(file)

            assertEquals(1, result.size)
            assertEquals("Only one line", result[0])
        } finally {
            file.delete()
        }
    }

    private fun createTempTxt(lines: List<String>): File {
        val file = File.createTempFile("test", ".txt")
        file.writeText(lines.joinToString(System.lineSeparator()))
        return file
    }
}
