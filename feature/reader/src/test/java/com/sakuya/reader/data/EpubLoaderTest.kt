package com.sakuya.reader.data

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EpubLoaderTest {

    private val loader = EpubLoader()

    @Test
    fun `load should parse single chapter epub correctly`() {
        val epubFile = createTestEpubFile()
        try {
            val result = loader.load(epubFile.inputStream())
            assertTrue(result.isNotEmpty())
            assertTrue(result.any { it.contains("Hello World") })
        } finally {
            epubFile.delete()
        }
    }

    private fun createTestEpubFile(): File {
        val file = File.createTempFile("test_epub", ".epub")
        file.outputStream().use { output ->
            output.write(createMinimalEpubBytes())
        }
        return file
    }

    private fun createMinimalEpubBytes(): ByteArray {
        val bytes = java.io.ByteArrayOutputStream()
        val zip = java.util.zip.ZipOutputStream(bytes)

        zip.putNextEntry(java.util.zip.ZipEntry("mimetype"))
        zip.write("application/epub+zip".toByteArray())
        zip.closeEntry()

        zip.putNextEntry(java.util.zip.ZipEntry("META-INF/container.xml"))
        zip.write("""<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".trimIndent().toByteArray())
        zip.closeEntry()

        zip.putNextEntry(java.util.zip.ZipEntry("OEBPS/content.opf"))
        zip.write("""<?xml version="1.0"?>
<package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="book-id">
  <metadata>
    <dc:title xmlns:dc="http://purl.org/dc/elements/1.1/">Test Book</dc:title>
  </metadata>
  <manifest>
    <item id="chapter1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
  </manifest>
  <spine toc="ncx">
    <itemref idref="chapter1"/>
  </spine>
</package>""".trimIndent().toByteArray())
        zip.closeEntry()

        zip.putNextEntry(java.util.zip.ZipEntry("OEBPS/toc.ncx"))
        zip.write("""<?xml version="1.0"?>
<!DOCTYPE ncx PUBLIC "-//NISO//DTD ncx 2005-1//EN" "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd">
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <docTitle><text>Test Book</text></docTitle>
  <navMap>
    <navPoint id="navpoint-1" playOrder="1">
      <navLabel><text>Chapter 1</text></navLabel>
      <content src="chapter1.xhtml"/>
    </navPoint>
  </navMap>
</ncx>""".trimIndent().toByteArray())
        zip.closeEntry()

        zip.putNextEntry(java.util.zip.ZipEntry("OEBPS/chapter1.xhtml"))
        zip.write("""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Chapter 1</title></head>
<body><p>Hello World, this is the first chapter.</p></body>
</html>""".trimIndent().toByteArray())
        zip.closeEntry()

        zip.close()
        return bytes.toByteArray()
    }
}
