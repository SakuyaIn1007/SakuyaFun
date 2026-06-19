package com.sakuya.reader.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.reader.data.ReaderPrefs
import com.sakuya.reader.data.repository.ReaderRepository
import com.sakuya.reader.model.ReaderDocument
import com.sakuya.reader.model.ReaderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val readerRepository: ReaderRepository,
    private val readerPrefs: ReaderPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState = _uiState.asStateFlow()

    fun openFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    progress = 0f,
                    errorMessage = null
                )
            }
            val bookKey = uri.toString()
            val document = readerRepository.openDocument(uri)
            val savedProgress = readerPrefs.gerProgress(bookKey)
            val savedFontSize = readerPrefs.getFontSize()
            _uiState.update {
                it.copy(
                    type = when (document) {
                        is ReaderDocument.Epub -> ReaderType.EPUB
                        else -> ReaderType.TXT
                    },
                    document = document,
                    isLoading = false,
                    progress = savedProgress,
                    fontSizeSp = savedFontSize,
                    errorMessage = if (document == null) {
                        "文件打开失败或暂不支持改格式"
                    } else {
                        null
                    },
                    bookKey = bookKey
                )
            }
        }
    }

    fun setProgress(progress: Float) {
        val safeProgress = progress.coerceIn(0f, 1f)
        _uiState.update {
            it.copy(progress = progress.coerceIn(0f, 1f))
        }

        val bookKey = _uiState.value.bookKey ?: return
        readerPrefs.saveProgress(bookKey, safeProgress)
    }

    fun toggleUI() {
        _uiState.update {
            it.copy(showUI = !it.showUI)
        }
    }

    fun changeFontSize(newSize: Float) {
        _uiState.update {
            it.copy(fontSizeSp = newSize.coerceIn(12f, 32f))
        }
    }

    fun saveProgress() {

    }

    fun restoreProgress() {

    }
}

data class ReaderUiState(
    val type: ReaderType = ReaderType.TXT,
    val document: ReaderDocument? = null,
    val progress: Float = 0f,
    val showUI: Boolean = true,
    val isLoading: Boolean = false,
    val fontSizeSp: Float = 18f,
    val errorMessage: String? =null,
    //
    val bookKey: String? = null
)
