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
import com.sakuya.reader.model.ReaderReadingPosition
import com.sakuya.reader.model.ReaderSessionState
import com.sakuya.reader.model.ReaderTheme
import com.sakuya.reader.model.ReaderType
import com.sakuya.reader.model.Wenku8NovelOpenResult
import com.sakuya.data.reading.ReadingSyncStatus
import com.sakuya.data.offline.OfflineDownloadState
import com.sakuya.data.offline.OfflineDownloadStatus
import com.sakuya.reader.tts.TtsPlaybackState
import com.sakuya.reader.tts.TtsPlaybackStatus
import com.sakuya.reader.data.EpubSearch
import com.sakuya.reader.data.EpubSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
/**
 * ReaderViewModel.kt
 * 职责说明：统一管理本地文件和后端内容库会话的阅读状态、进度、书签及连续阅读降级流程。
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
    private var observeDownloadJob: Job? = null
    private var remoteRequest: RemoteReadRequest? = null

    init {
        viewModelScope.launch {
            readerRepository.syncStatus.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
        viewModelScope.launch {
            readerRepository.syncError.collect { error ->
                _uiState.update { it.copy(syncErrorMessage = error) }
            }
        }
        viewModelScope.launch { readerRepository.ttsPlayback.collect { value -> _uiState.update { it.copy(ttsPlayback = value) } } }
    }

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
                setProgress(action.progress)
            }

            is ReaderAction.SetReadingPosition -> setReadingPosition(action.position)

            is ReaderAction.ChangeFontSize -> {
                changeFontSize(action.fontSize)
            }

            is ReaderAction.ChangeTheme -> changeTheme(action.theme)
            is ReaderAction.DeleteBookmark -> deleteBookmark(action.bookmarkId)
            is ReaderAction.JumpToBookmark -> jumpToBookmark(action.bookmark)
            ReaderAction.OpenSettingsPanel -> _uiState.update { it.copy(isSettingsPanelVisible = true) }
            ReaderAction.CloseSettingsPanel -> _uiState.update { it.copy(isSettingsPanelVisible = false) }
            ReaderAction.OpenBookmarksPanel -> _uiState.update { it.copy(isBookmarkPanelVisible = true) }
            ReaderAction.CloseBookmarksPanel -> _uiState.update { it.copy(isBookmarkPanelVisible = false) }
            ReaderAction.SaveReadingPosition -> flushReadingPosition()
            ReaderAction.RetrySync -> retrySync()
            ReaderAction.DownloadOffline -> manageOfflineDownload()
            ReaderAction.PauseOfflineDownload -> updateOffline { readerRepository.pauseOfflineDownload(it) }
            ReaderAction.ResumeOfflineDownload -> updateOffline { readerRepository.resumeOfflineDownload(it) }
            ReaderAction.RetryOfflineDownload -> updateOffline { readerRepository.retryOfflineDownload(it) }
            ReaderAction.DeleteOfflineDownload -> updateOffline { readerRepository.deleteOfflineDownload(it) }
            ReaderAction.ToggleTts -> toggleTts()
            ReaderAction.StopTts -> readerRepository.stopTts()
            is ReaderAction.ChangeTtsRate -> _uiState.update { it.copy(ttsRate = action.rate.coerceIn(0.5f, 2f)) }
            ReaderAction.OpenEpubSearch -> _uiState.update { it.copy(isEpubSearchVisible = true) }
            ReaderAction.CloseEpubSearch -> _uiState.update { it.copy(isEpubSearchVisible = false) }
            is ReaderAction.SearchEpub -> searchEpub(action.query)
            is ReaderAction.JumpToEpubSearchResult -> jumpToEpubSearchResult(action.result)
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
                    errorMessage = null,
                    sessionState = ReaderSessionState.Loading,
                    isSettingsPanelVisible = false,
                    isBookmarkPanelVisible = false,
                )
            }
            val openResult = readerRepository.openDocument(uri)
            val savedPosition = readerRepository.getReadingPosition(bookKey)
            val savedFontSize = readerPrefs.getFontSize()
            val savedTheme = readerPrefs.getTheme()
            val document = (openResult as? ReaderOpenResult.Success)?.document
            val documentType = when (document) {
                is ReaderDocument.Epub -> ReaderType.EPUB
                else -> ReaderType.TXT
            }
            _uiState.update {
                it.copy(
                    type = documentType,
                    document = document,
                    isLoading = false,
                    progress = savedPosition.progress,
                    readingPosition = savedPosition.copy(type = documentType),
                    fontSizeSp = savedFontSize,
                    theme = savedTheme,
                    parseError = (openResult as? ReaderOpenResult.Failure)?.error,
                    errorMessage = (openResult as? ReaderOpenResult.Failure)?.error?.toDisplayMessage(),
                    bookKey = bookKey,
                    targetChapterId = null,
                    remoteNotice = null,
                    persistProgress = true,
                    currentChapterTitle = document?.title.orEmpty(),
                    positionRestoreRequest = it.positionRestoreRequest + 1,
                    sessionState = if (document != null) {
                        ReaderSessionState.Reading
                    } else {
                        ReaderSessionState.Error((openResult as? ReaderOpenResult.Failure)?.error?.toDisplayMessage() ?: "打开阅读文件失败")
                    },
                )
            }
            if (document != null) observeBookmarks(bookKey)
        }
    }

    /**
     * 同一小说使用 content:{bookId} 保存连续进度，并在首次打开时迁移旧 wenku8:* 数据。
     * 目录传入的章节始终优先于旧进度；
     * 全文失败时只展示当前章，且不覆写这份连续进度，等待用户稍后重试全文。
     */
    private fun openRemoteNovel(novelId: String, chapterId: String, title: String) {
        val request = RemoteReadRequest(novelId, chapterId, title)
        remoteRequest = request
        saveProgressJob?.cancel()
        openRemoteJob?.cancel()
        openRemoteJob = viewModelScope.launch {
            val bookKey = readerRepository.migrateLegacyRemoteState(novelId)
            observeOfflineDownload(bookKey)
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
                    persistProgress = false,
                    sessionState = ReaderSessionState.Loading,
                )
            }
            // 正文请求与云状态同步并行；同步最多占用 8 秒，失败后仍使用本地缓存继续阅读。
            val contentRequest = async { readerRepository.openRemoteNovel(novelId, chapterId, title) }
            withTimeoutOrNull(8_000) { readerRepository.syncOnlineState() }
            when (val result = contentRequest.await()) {
                is Wenku8NovelOpenResult.Full -> {
                    val savedPosition = readerRepository.getReadingPosition(bookKey)
                    _uiState.update {
                        it.copy(
                            document = result.document,
                            type = ReaderType.WENKU8,
                            isLoading = false,
                            progress = savedPosition.progress,
                            readingPosition = savedPosition.copy(type = ReaderType.WENKU8),
                            targetChapterId = chapterId,
                            persistProgress = true,
                            sessionState = ReaderSessionState.Reading,
                            currentChapterTitle = title,
                            positionRestoreRequest = it.positionRestoreRequest + 1,
                        )
                    }
                    observeBookmarks(bookKey)
                }
                is Wenku8NovelOpenResult.ChapterFallback -> {
                    _uiState.update {
                        it.copy(
                            document = result.document,
                            type = ReaderType.WENKU8,
                            isLoading = false,
                            progress = 0f,
                            targetChapterId = null,
                            remoteNotice = result.notice,
                            persistProgress = false,
                            sessionState = ReaderSessionState.Reading,
                            readingPosition = ReaderReadingPosition(type = ReaderType.WENKU8),
                            currentChapterTitle = title,
                            positionRestoreRequest = it.positionRestoreRequest + 1,
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
                            errorMessage = result.error.toDisplayMessage(),
                            sessionState = ReaderSessionState.Error(result.error.toDisplayMessage()),
                        )
                    }
                }
            }
        }
    }

    private fun addBookmark() {
        val bookKey = _uiState.value.bookKey ?: return
        val state = _uiState.value

        viewModelScope.launch {
            readerRepository.addBookmark(
                bookId = bookKey,
                position = state.readingPosition,
                title = state.currentChapterTitle.ifBlank {
                    "进度 ${(state.readingPosition.progress * 100).toInt()}%"
                },
            )
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

    private fun deleteBookmark(bookmarkId: String) {
        val bookKey = _uiState.value.bookKey ?: return
        viewModelScope.launch { readerRepository.deleteBookmark(bookKey, bookmarkId) }
    }

    private fun retrySync() {
        if (_uiState.value.syncStatus == ReadingSyncStatus.SYNCING) return
        viewModelScope.launch {
            val result = readerRepository.syncOnlineState()
            _effect.emit(
                ReaderEffect.ShowMessage(
                    if (result.isSuccess) "阅读进度与书签已同步"
                    else result.exceptionOrNull()?.message ?: "同步失败，请稍后重试"
                )
            )
        }
    }

    private fun observeOfflineDownload(bookKey: String) {
        observeDownloadJob?.cancel()
        observeDownloadJob = viewModelScope.launch {
            readerRepository.observeOfflineDownload(bookKey).collect { download ->
                val bytes = runCatching { readerRepository.offlineStorageBytes() }.getOrDefault(0L)
                _uiState.update { it.copy(offlineDownload = download, offlineStorageBytes = bytes) }
            }
        }
    }

    /** 顶栏主按钮按当前状态执行入队或恢复，暂停、重试和删除作为独立显式动作。 */
    private fun manageOfflineDownload() {
        val state = _uiState.value
        val bookKey = state.bookKey ?: return
        viewModelScope.launch {
            runCatching {
                when (state.offlineDownload?.status) {
                    OfflineDownloadStatus.PAUSED -> readerRepository.resumeOfflineDownload(bookKey)
                    OfflineDownloadStatus.FAILED -> readerRepository.retryOfflineDownload(bookKey)
                    else -> readerRepository.enqueueOfflineDownload(bookKey, state.document?.title.orEmpty().ifBlank { state.currentChapterTitle })
                }
            }.onFailure { _effect.emit(ReaderEffect.ShowMessage(it.message ?: "离线下载操作失败")) }
        }
    }

    private fun updateOffline(block: suspend (String) -> Unit) {
        val bookKey = _uiState.value.bookKey ?: return
        viewModelScope.launch { runCatching { block(bookKey) }.onFailure { _effect.emit(ReaderEffect.ShowMessage(it.message ?: "离线下载操作失败")) } }
    }

    private fun toggleTts() {
        when (_uiState.value.ttsPlayback.status) {
            TtsPlaybackStatus.PLAYING, TtsPlaybackStatus.PREPARING -> readerRepository.pauseTts()
            TtsPlaybackStatus.PAUSED -> readerRepository.resumeTts()
            else -> {
                val state = _uiState.value
                val (text, startProgress) = when (val document = state.document) {
                    is ReaderDocument.Txt -> document.text to state.readingPosition.progress
                    is ReaderDocument.Wenku8Full -> document.text to state.readingPosition.progress
                    is ReaderDocument.Epub -> document.chapters.drop(state.readingPosition.chapterIndex).joinToString("\n\n") { it.content } to state.readingPosition.chapterProgress
                    null -> "" to 0f
                }
                if (text.isBlank()) return
                viewModelScope.launch { readerRepository.startTts(documentTitle(state), text, startProgress, state.ttsRate) }
            }
        }
    }

    private fun documentTitle(state: ReaderUiState) = state.document?.title.orEmpty().ifBlank { state.currentChapterTitle.ifBlank { "阅读朗读" } }

    private fun searchEpub(query: String) {
        _uiState.update { it.copy(epubSearchQuery = query) }
        val document = _uiState.value.document as? ReaderDocument.Epub ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val results = EpubSearch.search(document.chapters, query)
            _uiState.update { if (it.epubSearchQuery == query) it.copy(epubSearchResults = results) else it }
        }
    }

    private fun jumpToEpubSearchResult(result: EpubSearchResult) {
        val document = _uiState.value.document as? ReaderDocument.Epub ?: return
        val global = result.chapterIndex.toFloat() / document.chapters.size.coerceAtLeast(1)
        _uiState.update {
            it.copy(
                readingPosition = ReaderReadingPosition(ReaderType.EPUB, global, chapterIndex = result.chapterIndex),
                progress = global,
                currentChapterTitle = result.chapterTitle,
                isEpubSearchVisible = false,
                positionRestoreRequest = it.positionRestoreRequest + 1,
            )
        }
    }

    private fun setProgress(progress: Float) {
        setReadingPosition(_uiState.value.readingPosition.copy(progress = progress))
    }

    /** 组件回传的统一位置同时更新兼容 progress，并以短防抖写入 Room。 */
    private fun setReadingPosition(position: ReaderReadingPosition) {
        val safePosition = position.normalized
        _uiState.update { state ->
            state.copy(
                readingPosition = safePosition,
                progress = safePosition.progress,
            )
        }
        val bookKey = _uiState.value.bookKey ?: return
        if (!_uiState.value.persistProgress) return
        saveProgressJob?.cancel()
        saveProgressJob = viewModelScope.launch {
            delay(300)
            readerRepository.saveReadingPosition(bookKey, safePosition)
        }
    }

    /** 供生命周期和页面离开事件调用；比防抖任务更优先，避免最后几秒进度丢失。 */
    private fun flushReadingPosition() {
        val state = _uiState.value
        val bookKey = state.bookKey ?: return
        if (!state.persistProgress) return
        saveProgressJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            readerRepository.saveReadingPosition(bookKey, state.readingPosition)
        }
    }

    /** 跳转书签时只变更状态，具体滚动定位由三种正文 Composable 根据读取位置完成。 */
    private fun jumpToBookmark(bookmark: ReaderBookmark) {
        val current = _uiState.value
        val position = current.readingPosition.copy(
            progress = bookmark.progress,
            chapterId = bookmark.chapterId,
            chapterProgress = bookmark.chapterProgress,
        ).normalized
        _uiState.update {
            it.copy(
                readingPosition = position,
                progress = position.progress,
                targetChapterId = bookmark.chapterId,
                isBookmarkPanelVisible = false,
                // 仅书签跳转和新会话递增该标记，正文不会因每次滚动回传而反复跳回旧位置。
                positionRestoreRequest = it.positionRestoreRequest + 1,
            )
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

    override fun onCleared() {
        // 页面生命周期已优先触发 SaveReadingPosition；此处作为 ViewModel 销毁时的最后保障。
        val state = _uiState.value
        val bookKey = state.bookKey
        if (bookKey != null && state.persistProgress) {
            viewModelScope.launch(Dispatchers.IO + NonCancellable) {
                readerRepository.saveReadingPosition(bookKey, state.readingPosition)
            }
        }
        super.onCleared()
    }
}

data class ReaderUiState(
    val type: ReaderType = ReaderType.TXT,
    val document: ReaderDocument? = null,
    val progress: Float = 0f,
    val readingPosition: ReaderReadingPosition = ReaderReadingPosition(),
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
    val persistProgress: Boolean = true,
    val sessionState: ReaderSessionState = ReaderSessionState.Idle,
    val isSettingsPanelVisible: Boolean = false,
    val isBookmarkPanelVisible: Boolean = false,
    /** UI 顶栏和书签标题统一使用，远端章节定位时优先展示目录标题。 */
    val currentChapterTitle: String = "",
    /** 触发正文组件执行一次恢复/书签跳转，避免将持续更新的 progress 直接作为定位副作用的 key。 */
    val positionRestoreRequest: Long = 0L,
    val syncStatus: ReadingSyncStatus = ReadingSyncStatus.SYNCED,
    val syncErrorMessage: String? = null,
    val offlineDownload: OfflineDownloadState? = null,
    val offlineStorageBytes: Long = 0,
    val ttsPlayback: TtsPlaybackState = TtsPlaybackState(),
    val ttsRate: Float = 1f,
    val isEpubSearchVisible: Boolean = false,
    val epubSearchQuery: String = "",
    val epubSearchResults: List<EpubSearchResult> = emptyList(),
)

sealed interface ReaderEffect {
    data object OpenFilePicker : ReaderEffect
    data object BookmarkAdded : ReaderEffect
    data class ShowMessage(val message: String) : ReaderEffect
}

sealed interface ReaderAction {
    data class OpenFile(
        val uri: Uri,
        val bookId: String?
        ) : ReaderAction
    data class OpenRemoteChapter(val novelId: String, val chapterId: String, val title: String) : ReaderAction
    data class SetProgress(val progress: Float) : ReaderAction
    data class SetReadingPosition(val position: ReaderReadingPosition) : ReaderAction
    data class ChangeFontSize(val fontSize: Float) : ReaderAction
    data class ChangeTheme(val theme: ReaderTheme) : ReaderAction
    data class DeleteBookmark(val bookmarkId: String) : ReaderAction
    data class JumpToBookmark(val bookmark: ReaderBookmark) : ReaderAction
    data object RetryRemoteRead : ReaderAction
    data object OpenFilePickerClick : ReaderAction
    data object ToggleUi : ReaderAction
    data object AddBookmark : ReaderAction
    data object OpenSettingsPanel : ReaderAction
    data object CloseSettingsPanel : ReaderAction
    data object OpenBookmarksPanel : ReaderAction
    data object CloseBookmarksPanel : ReaderAction
    data object SaveReadingPosition : ReaderAction
    data object RetrySync : ReaderAction
    data object DownloadOffline : ReaderAction
    data object PauseOfflineDownload : ReaderAction
    data object ResumeOfflineDownload : ReaderAction
    data object RetryOfflineDownload : ReaderAction
    data object DeleteOfflineDownload : ReaderAction
    data object ToggleTts : ReaderAction
    data object StopTts : ReaderAction
    data class ChangeTtsRate(val rate: Float) : ReaderAction
    data object OpenEpubSearch : ReaderAction
    data object CloseEpubSearch : ReaderAction
    data class SearchEpub(val query: String) : ReaderAction
    data class JumpToEpubSearchResult(val result: EpubSearchResult) : ReaderAction
}

/** 保存一次可重试的远端目录选择；不包含正文、Cookie 或任何上游登录信息。 */
private data class RemoteReadRequest(val novelId: String, val chapterId: String, val title: String)

/** 将底层错误转换为稳定的用户可读信息，UI 不需判断具体解析实现。 */
private fun ReaderParseError.toDisplayMessage(): String = when (this) {
    ReaderParseError.UnsupportedFormat -> "暂不支持该文件格式"
    ReaderParseError.FileNotFound -> "找不到阅读文件"
    ReaderParseError.FileTooLarge -> "文件过大，暂不支持打开"
    ReaderParseError.DownloadFailed -> "文件下载失败，请检查网络后重试"
    ReaderParseError.RemoteTimeout -> "远端正文加载超时，请稍后重试"
    ReaderParseError.EmptyDocument -> "文件内容为空"
    is ReaderParseError.InvalidContent -> detail ?: "文件内容损坏或无法解析"
}
