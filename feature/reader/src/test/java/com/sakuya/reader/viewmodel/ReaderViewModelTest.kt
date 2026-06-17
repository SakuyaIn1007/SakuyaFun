package com.sakuya.reader.viewmodel

import app.cash.turbine.test
import com.sakuya.reader.data.EpubLoader
import com.sakuya.reader.data.TxtLoader
import com.sakuya.reader.model.ReaderType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ReaderViewModelTest {

    private fun fakeEpubLoader(vararg chapters: String) = object : EpubLoader() {
        override fun load(file: File): List<String> = chapters.toList()
    }

    private fun fakeTxtLoader(vararg pages: String) = object : TxtLoader() {
        override fun load(file: File): List<String> = pages.toList()
    }

    @Test
    fun `openFile with txt extension should load as TXT type`() = runTest {
        val viewModel = ReaderViewModel(
            epubLoader = fakeEpubLoader(),
            txtLoader = fakeTxtLoader("Page 1 content")
        )

        viewModel.openFile(File("test.txt"))
        val state = viewModel.uiState.value

        assertEquals(ReaderType.TXT, state.type)
        assertEquals(listOf("Page 1 content"), state.pages)
        assertEquals(0, state.currentPage)
    }

    @Test
    fun `openFile with epub extension should load as EPUB type`() = runTest {
        val viewModel = ReaderViewModel(
            epubLoader = fakeEpubLoader("Chapter 1", "Chapter 2"),
            txtLoader = fakeTxtLoader()
        )

        viewModel.openFile(File("test.epub"))
        val state = viewModel.uiState.value

        assertEquals(ReaderType.EPUB, state.type)
        assertEquals(2, state.pages.size)
        assertEquals("Chapter 1", state.pages[0])
        assertEquals(0, state.currentPage)
    }

    @Test
    fun `nextPage should increment currentPage`() = runTest {
        val viewModel = ReaderViewModel(
            epubLoader = fakeEpubLoader("Page 1", "Page 2", "Page 3"),
            txtLoader = fakeTxtLoader()
        )
        viewModel.openFile(File("test.epub"))

        viewModel.uiState.test {
            assertEquals(0, awaitItem().currentPage)
            viewModel.nextPage()
            assertEquals(1, awaitItem().currentPage)
            viewModel.nextPage()
            assertEquals(2, awaitItem().currentPage)
        }
    }

    @Test
    fun `nextPage should not exceed last page`() = runTest {
        val viewModel = ReaderViewModel(
            epubLoader = fakeEpubLoader("Page 1", "Page 2"),
            txtLoader = fakeTxtLoader()
        )
        viewModel.openFile(File("test.epub"))

        viewModel.uiState.test {
            assertEquals(0, awaitItem().currentPage)
            viewModel.nextPage()
            assertEquals(1, awaitItem().currentPage)
        }
        // 已在最后一页，再 nextPage 不应再增加
        viewModel.nextPage()
        assertEquals(1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `prevPage should decrement currentPage`() = runTest {
        val viewModel = ReaderViewModel(
            epubLoader = fakeEpubLoader("Page 1", "Page 2", "Page 3"),
            txtLoader = fakeTxtLoader()
        )
        viewModel.openFile(File("test.epub"))
        viewModel.nextPage()
        viewModel.nextPage()

        viewModel.uiState.test {
            assertEquals(2, awaitItem().currentPage)
            viewModel.prevPage()
            assertEquals(1, awaitItem().currentPage)
            viewModel.prevPage()
            assertEquals(0, awaitItem().currentPage)
        }
    }

    @Test
    fun `prevPage should not go below zero`() = runTest {
        val viewModel = ReaderViewModel(
            epubLoader = fakeEpubLoader("Page 1"),
            txtLoader = fakeTxtLoader()
        )
        viewModel.openFile(File("test.epub"))

        viewModel.uiState.test {
            assertEquals(0, awaitItem().currentPage)
        }
        // 已在第一页，prevPage 不应再减少
        viewModel.prevPage()
        assertEquals(0, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `toggleUI should flip showUI state`() = runTest {
        val viewModel = ReaderViewModel(
            epubLoader = fakeEpubLoader(),
            txtLoader = fakeTxtLoader()
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().showUI)
            viewModel.toggleUI()
            assertFalse(awaitItem().showUI)
            viewModel.toggleUI()
            assertTrue(awaitItem().showUI)
        }
    }

    @Test
    fun `openFile should reset currentPage to zero`() = runTest {
        val viewModel = ReaderViewModel(
            epubLoader = fakeEpubLoader("Chapter 1", "Chapter 2"),
            txtLoader = fakeTxtLoader("Page 1")
        )
        viewModel.openFile(File("book.epub"))
        viewModel.nextPage()

        viewModel.openFile(File("another.txt"))
        val state = viewModel.uiState.value

        assertEquals(0, state.currentPage)
        assertEquals(ReaderType.TXT, state.type)
    }
}
