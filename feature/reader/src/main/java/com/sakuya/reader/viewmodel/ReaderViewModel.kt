package com.sakuya.reader.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.reader.data.ReaderPrefs
import com.sakuya.reader.data.repository.ReaderRepository
import com.sakuya.reader.model.ReaderDocument
import com.sakuya.reader.model.ReaderBookmark
import com.sakuya.reader.model.ReaderOpenResult
import com.sakuya.reader.model.ReaderParseError
import com.sakuya.reader.model.ReaderTheme
import com.sakuya.reader.model.ReaderType
import com.sakuya.reader.model.Wenku8NovelOpenResult
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
/**
 * ReaderViewModel.kt
 * 职责说明：统一管理本地文件和 Wenku8 会话阅读状态、进度、书签及连续阅读降级流程。
 * 执行流程：UI 发出打开/重试事件 -> ViewModel 在 viewModelScope 调用 Repository -> 更新不可变 UiState -> UI 渲染或定位章节。
 */
class ReaderViewModel @Inject constructor(
    private val readerRepository: ReaderRepository,
    private val readerPrefs: ReaderPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ReaderEffect>()
    val effect = _effect.asSharedFlow()

    private var saveProgressJob: Job? = null
    private var observeBookmarksJob: Job? = null
    private var openRemoteJob: Job? = null
    private var remoteRequest: RemoteReadRequest? = null

    fun onAction(action: ReaderAction) {
        when (action) {
            is ReaderAction.OpenFile -> {
                openFile(
                    uri = action.uri,
                    bookId = action.bookId
                )
            }
            is ReaderAction.OpenRemoteChapter -> openRemoteNovel(action.novelId, action.chapterId, action.title)
            ReaderAction.RetryRemoteRead -> remoteRequest?.let { openRemoteNovel(it.novelId, it.chapterId, it.title) }

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

            is ReaderAction.ChangeTheme -> changeTheme(action.theme)
            is ReaderAction.DeleteBookmark -> deleteBookmark(action.bookmarkId)
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
        openRemoteJob?.cancel()
        remoteRequest = null
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    progress = 0f,
                    errorMessage = null
                )
            }
            val openResult = readerRepository.openDocument(uri)
            val savedProgress = readerRepository.getProgress(bookKey)
            val savedFontSize = readerPrefs.getFontSize()
            val savedTheme = readerPrefs.getTheme()
            val document = (openResult as? ReaderOpenResult.Success)?.document
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
                    theme = savedTheme,
                    parseError = (openResult as? ReaderOpenResult.Failure)?.error,
                    errorMessage = (openResult as? ReaderOpenResult.Failure)?.error?.toDisplayMessage(),
                    bookKey = bookKey,
                    targetChapterId = null,
                    remoteNotice = null,
                    persistProgress = true
                )
            }
            if (document != null) observeBookmarks(bookKey)
        }
    }

    /**
     * 同一小说只使用 wenku8:{novelId} 一份连续进度。目录传入的章节始终优先于旧进度；
     * 全文失败时只展示当前章，且不覆写这份连续进度，等待用户稍后重试全文。
     */
    private fun openRemoteNovel(novelId: String, chapterId: String, title: String) {
        val request = RemoteReadRequest(novelId, chapterId, title)
        val bookKey = "wenku8:$novelId"
        remoteRequest = request
        saveProgressJob?.cancel()
        openRemoteJob?.cancel()
        openRemoteJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    document = null,
                    errorMessage = null,
                    parseError = null,
                    remoteNotice = null,
                    progress = 0f,
                    bookKey = bookKey,
                    targetChapterId = chapterId,
                    persistProgress = false
                )
            }
            when (val result = readerRepository.openRemoteNovel(novelId, chapterId, title)) {
                is Wenku8NovelOpenResult.Full -> {
                    val savedProgress = readerRepository.getProgress(bookKey)
                    _uiState.update {
                        it.copy(
                            document = result.document,
                            type = ReaderType.TXT,
                            isLoading = false,
                            progress = savedProgress,
                            targetChapterId = chapterId,
                            persistProgress = true
                        )
                    }
                    observeBookmarks(bookKey)
                }
                is Wenku8NovelOpenResult.ChapterFallback -> {
                    _uiState.update {
                        it.copy(
                            document = result.document,
                            type = ReaderType.TXT,
                            isLoading = false,
                            progress = 0f,
                            targetChapterId = null,
                            remoteNotice = result.notice,
                            persistProgress = false
                        )
                    }
                    observeBookmarks(bookKey)
                }
                is Wenku8NovelOpenResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            document = null,
                            isLoading = false,
                            targetChapterId = null,
                            persistProgress = false,
                            parseError = result.error,
                            errorMessage = result.error.toDisplayMessage()
                        )
                    }
                }
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

    private fun observeBookmarks(bookKey: String) {
        observeBookmarksJob?.cancel()
        observeBookmarksJob = viewModelScope.launch {
            readerRepository.observeBookmarks(bookKey).collect { bookmarks ->
                _uiState.update { it.copy(bookmarks = bookmarks) }
            }
        }
    }

    private fun deleteBookmark(bookmarkId: String) = viewModelScope.launch { readerRepository.deleteBookmark(bookmarkId) }

    private fun setProgress(progress: Float) {
        val safeProgress = progress.coerceIn(0f, 1f)
        _uiState.update {
            it.copy(progress = safeProgress)
        }
        val bookKey = _uiState.value.bookKey ?: return
        if (!_uiState.value.persistProgress) return
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

    private fun changeTheme(theme: ReaderTheme) {
        _uiState.update { it.copy(theme = theme) }
        readerPrefs.saveTheme(theme)
    }
}

data class ReaderUiState(
    val type: ReaderType = ReaderType.TXT,
    val document: ReaderDocument? = null,
    val progress: Float = 0f,
    val showUI: Boolean = true,
    val isLoading: Boolean = false,
    val fontSizeSp: Float = 18f,
    val theme: ReaderTheme = ReaderTheme.SYSTEM,
    val bookmarks: List<ReaderBookmark> = emptyList(),
    val parseError: ReaderParseError? = null,
    val errorMessage: String? = null,
    val bookKey: String? = null,
    /** 仅 Wenku8 全文使用；非空时全文 UI 在恢复旧进度前先定位目录选中的章节。 */
    val targetChapterId: String? = null,
    /** 全文受限时明确提示已降级为单章阅读。 */
    val remoteNotice: String? = null,
    val persistProgress: Boolean = true
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
    data class OpenRemoteChapter(val novelId: String, val chapterId: String, val title: String) : ReaderAction
    data class SetProgress(val progress: Float) : ReaderAction
    data class ChangeFontSize(val fontSize: Float) : ReaderAction
    data class ChangeTheme(val theme: ReaderTheme) : ReaderAction
    data class DeleteBookmark(val bookmarkId: String) : ReaderAction
    data object RetryRemoteRead : ReaderAction
    data object OpenFilePickerClick : ReaderAction
    data object ToggleUi : ReaderAction
    data object AddBookmark : ReaderAction
}

/** 保存一次可重试的远端目录选择；不包含正文、Cookie 或任何上游登录信息。 */
private data class RemoteReadRequest(val novelId: String, val chapterId: String, val title: String)

/** 将底层错误转换为稳定的用户可读信息，UI 不需判断具体解析实现。 */
private fun ReaderParseError.toDisplayMessage(): String = when (this) {
    ReaderParseError.UnsupportedFormat -> "暂不支持该文件格式"
    ReaderParseError.FileNotFound -> "找不到阅读文件"
    ReaderParseError.DownloadFailed -> "文件下载失败，请检查网络后重试"
    ReaderParseError.EmptyDocument -> "文件内容为空"
    is ReaderParseError.InvalidContent -> detail ?: "文件内容损坏或无法解析"
}
