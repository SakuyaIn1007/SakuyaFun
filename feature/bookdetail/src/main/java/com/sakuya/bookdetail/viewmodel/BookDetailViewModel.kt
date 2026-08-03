package com.sakuya.bookdetail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.bookdetail.data.repository.BookDetailRepository
import com.sakuya.data.catalog.CatalogBook
import com.sakuya.library.data.repository.LibraryRepository
import com.sakuya.library.model.LibraryItem
import com.sakuya.navigation.BOOK_DETAIL_ARG_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val bookDetailRepository: BookDetailRepository
) : ViewModel() {
    private val bookId: String = savedStateHandle[BOOK_DETAIL_ARG_ID] ?: ""

    private val _uiState = MutableStateFlow(BookDetailUiState(bookId = bookId))
    val uiState = _uiState.asStateFlow()

    init {
        observeBook()
        loadBookDetail()
    }

    private fun observeBook() {
        viewModelScope.launch {
            libraryRepository.observeBooks().collect { books ->
                val book = books.firstOrNull { it.id == bookId }
                _uiState.value = _uiState.value.copy(book = book)
            }
        }
    }

    private fun loadBookDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            bookDetailRepository.getBook(bookId)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        catalogBook = result.book,
                        isLoading = false
                    )
                    if (result.isCollected && _uiState.value.book == null) {
                        runCatching { libraryRepository.syncLibrary() }
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "加载书籍详情失败"
                    )
                }
        }
    }

    fun addToLibrary() {
        val book = _uiState.value.catalogBook ?: return
        if (_uiState.value.isAddingToLibrary || _uiState.value.book != null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingToLibrary = true, errorMessage = null)
            runCatching { libraryRepository.addCatalogBook(book) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isAddingToLibrary = false)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isAddingToLibrary = false,
                        errorMessage = error.message ?: "加入书架失败，请稍后重试"
                    )
                }
        }
    }
}

data class BookDetailUiState(
    val bookId: String = "",
    val catalogBook: CatalogBook? = null,
    val book: LibraryItem? = null,
    val isLoading: Boolean = true,
    val isAddingToLibrary: Boolean = false,
    val errorMessage: String? = null
)
