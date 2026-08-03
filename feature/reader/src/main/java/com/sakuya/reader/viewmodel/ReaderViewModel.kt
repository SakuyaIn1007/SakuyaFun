package com.sakuya.reader.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.reader.data.ReaderPrefs
import com.sakuya.reader.data.repository.ReaderRepository
import com.sakuya.reader.model.ReaderDocument
import com.sakuya.reader.model.ReaderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _effect = MutableSharedFlow<ReaderEffect>()
    val effect = _effect.asSharedFlow()

    private var saveProgressJob: Job? = null

    fun onAction(action: ReaderAction) {
        when (action) {
            is ReaderAction.OpenFile -> {
                openFile(
                    uri = action.uri,
                    bookId = action.bookId
                )
            }

            is ReaderAction.OpenFilePickerClick -> {
                emitEffect(ReaderEffect.OpenFilePicker)
            }

            is ReaderAction.ToggleUi -> {
                toggleUI()
            }

            is ReaderAction.AddBookmark -> {
                addBookmark()
            }

            is ReaderAction.SetProgress -> {
                setProgress(
                    progress = action.progress
                )
            }

            is ReaderAction.ChangeFontSize -> {
                changeFontSize(action.fontSize)
            }
        }
    }

    private fun emitEffect(effect: ReaderEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }

    private fun openFile(uri: Uri, bookId: String?) {
        val bookKey = bookId ?: uri.toString()
        saveProgressJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    progress = 0f,
                    errorMessage = null
                )
            }
            val document = readerRepository.openDocument(uri)
            val savedProgress = readerRepository.getProgress(bookKey)
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

    private fun addBookmark() {
        val bookKey = _uiState.value.bookKey ?: return
        val progress = _uiState.value.progress

        viewModelScope.launch {
            readerRepository.addBookmark(bookKey, progress)
            _effect.emit(ReaderEffect.BookmarkAdded)
        }
    }

    private fun setProgress(progress: Float) {
        val safeProgress = progress.coerceIn(0f, 1f)
        _uiState.update {
            it.copy(progress = safeProgress)
        }
        val bookKey = _uiState.value.bookKey ?: return
        saveProgressJob?.cancel()
        saveProgressJob = viewModelScope.launch {
            delay(300)
            readerRepository.saveProgress(bookKey, safeProgress)
        }
    }

    private fun toggleUI() {
        _uiState.update {
            it.copy(showUI = !it.showUI)
        }
    }

    private fun changeFontSize(newSize: Float) {
        val safeFontSize = newSize.coerceIn(12f, 32f)
        _uiState.update {
            it.copy(fontSizeSp = safeFontSize)
        }
        readerPrefs.saveFontSize(safeFontSize)
    }
}

data class ReaderUiState(
    val type: ReaderType = ReaderType.TXT,
    val document: ReaderDocument? = null,
    val progress: Float = 0f,
    val showUI: Boolean = true,
    val isLoading: Boolean = false,
    val fontSizeSp: Float = 18f,
    val errorMessage: String? = null,
    val bookKey: String? = null
)

sealed interface ReaderEffect {
    data object OpenFilePicker : ReaderEffect
    data object BookmarkAdded : ReaderEffect
}

sealed interface ReaderAction {
    data class OpenFile(
        val uri: Uri,
        val bookId: String?
        ) : ReaderAction
    data class SetProgress(val progress: Float) : ReaderAction
    data class ChangeFontSize(val fontSize: Float) : ReaderAction
    data object OpenFilePickerClick : ReaderAction
    data object ToggleUi : ReaderAction
    data object AddBookmark : ReaderAction
}
