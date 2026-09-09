package com.sakuya.reader.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sakuya.reader.model.ReaderChapter
import com.sakuya.reader.model.ReaderDocument
import com.sakuya.reader.model.ReaderSessionState
import com.sakuya.reader.model.ReaderTheme
import com.sakuya.reader.model.ReaderType
import com.sakuya.reader.ui.components.ReaderBottomBar
import com.sakuya.reader.ui.components.ReaderBookmarkSheet
import com.sakuya.reader.ui.components.ReaderTopBar
import com.sakuya.reader.ui.components.EpubSearchSheet
import com.sakuya.reader.ui.subpages.EpubReaderContent
import com.sakuya.reader.ui.subpages.TxtReaderContent
import com.sakuya.reader.ui.subpages.Wenku8FullReaderContent
import com.sakuya.reader.viewmodel.ReaderAction
import com.sakuya.reader.viewmodel.ReaderEffect
import com.sakuya.reader.viewmodel.ReaderUiState
import com.sakuya.reader.viewmodel.ReaderViewModel
import com.sakuya.data.reading.ReadingSyncStatus
import com.sakuya.data.offline.OfflineDownloadStatus
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import java.io.File

@Composable
fun ReaderScreen(
    bookId: String?,
    filePath: String,
    remoteChapter: Triple<String, String, String>? = null,
    onBack: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var currentFileLabel by remember { mutableStateOf(if (filePath.isNotEmpty()) File(filePath).name else "") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            currentFileLabel = uri.lastPathSegment ?: ""
            viewModel.onAction(
                ReaderAction.OpenFile(
                    uri = uri,
                    bookId = null
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ReaderEffect.OpenFilePicker -> {
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/epub+zip",
                            "text/plain",
                            "*/*"
                        )
                    )
                }

                ReaderEffect.BookmarkAdded -> snackbarHostState.showSnackbar("已添加书签")
                is ReaderEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(filePath) {
        if (filePath.isNotEmpty()) {
            currentFileLabel = File(filePath).name
            val fileUri = if (filePath.startsWith("content://")) {
                Uri.parse(filePath)
            } else {
                Uri.fromFile(File(filePath))
            }
            viewModel.onAction(
                ReaderAction.OpenFile(
                    uri = fileUri,
                    bookId = bookId
                )
            )
        }
    }
    LaunchedEffect(remoteChapter) { remoteChapter?.let { viewModel.onAction(ReaderAction.OpenRemoteChapter(it.first, it.second, it.third)) } }

    /**
     * 阅读进度的防抖保存覆盖正常滚动；应用进入后台和页面离开时额外强制保存，
     * 避免用户刚翻页就退出导致最后位置丢失。
     */
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAction(ReaderAction.SaveReadingPosition)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onAction(ReaderAction.SaveReadingPosition)
        }
    }

    ReaderContent(
        state = state,
        // 远端正文加载失败时 document 为空，仍保留目录传入的标题，让返回栏和错误页可辨识。
        bookTitle = state.document?.title ?: remoteChapter?.third ?: currentFileLabel,
        onBack = onBack,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun ReaderContent(
    state: ReaderUiState,
    bookTitle: String = "",
    onBack: () -> Unit = {},
    onAction: (ReaderAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val readerColors = readerScaffoldColors(state.theme)

    Scaffold(
        containerColor = readerColors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AnimatedVisibility(state.showUI && state.sessionState !is ReaderSessionState.Loading) {
                ReaderTopBar(
                    title = bookTitle,
                    onBack = onBack,
                    containerColor = readerColors.background,
                    contentColor = readerColors.content,
                    actions = {
                        if (state.bookKey?.startsWith("content:") == true) {
                            TextButton(
                                onClick = { onAction(ReaderAction.RetrySync) },
                                enabled = state.syncStatus != ReadingSyncStatus.SYNCING,
                            ) {
                                Text(
                                    text = when (state.syncStatus) {
                                        ReadingSyncStatus.SYNCED -> "已同步"
                                        ReadingSyncStatus.PENDING -> "待同步"
                                        ReadingSyncStatus.SYNCING -> "同步中"
                                        ReadingSyncStatus.FAILED -> "同步失败"
                                    },
                                    color = if (state.syncStatus == ReadingSyncStatus.FAILED) MaterialTheme.colorScheme.error else readerColors.content,
                                )
                            }
                            val download = state.offlineDownload
                            IconButton(
                                onClick = {
                                    onAction(
                                        when (download?.status) {
                                            OfflineDownloadStatus.DOWNLOADING -> ReaderAction.PauseOfflineDownload
                                            OfflineDownloadStatus.PAUSED -> ReaderAction.ResumeOfflineDownload
                                            OfflineDownloadStatus.FAILED -> ReaderAction.RetryOfflineDownload
                                            else -> ReaderAction.DownloadOffline
                                        }
                                    )
                                },
                                enabled = download?.status != OfflineDownloadStatus.COMPLETED,
                            ) {
                                Icon(
                                    imageVector = when (download?.status) {
                                        OfflineDownloadStatus.DOWNLOADING -> Icons.Default.Close
                                        OfflineDownloadStatus.PAUSED, OfflineDownloadStatus.FAILED -> Icons.Default.Refresh
                                        else -> Icons.Default.Add
                                    },
                                    contentDescription = when (download?.status) {
                                        OfflineDownloadStatus.DOWNLOADING -> "暂停离线下载"
                                        OfflineDownloadStatus.PAUSED -> "继续离线下载"
                                        OfflineDownloadStatus.FAILED -> "重试离线下载"
                                        OfflineDownloadStatus.COMPLETED -> "离线内容已完成"
                                        else -> "下载离线内容"
                                    },
                                    tint = if (download?.status == OfflineDownloadStatus.FAILED) MaterialTheme.colorScheme.error else readerColors.content,
                                )
                            }
                            if (download != null) {
                                Text(
                                    text = if (download.status == OfflineDownloadStatus.COMPLETED) "离线 ${"%.1f".format(state.offlineStorageBytes / 1024f / 1024f)}M" else "${(download.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = readerColors.content,
                                )
                                IconButton(onClick = { onAction(ReaderAction.DeleteOfflineDownload) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除离线内容", tint = readerColors.content)
                                }
                            }
                        }
                        IconButton(onClick = { onAction(ReaderAction.AddBookmark) }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "添加书签",
                                modifier = Modifier.size(24.dp),
                                tint = readerColors.content,
                            )
                        }
                        IconButton(onClick = { onAction(ReaderAction.OpenBookmarksPanel) }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "查看书签",
                                modifier = Modifier.size(24.dp),
                                tint = readerColors.content,
                            )
                        }
                        IconButton(onClick = { onAction(ReaderAction.OpenFilePickerClick) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "选择文件",
                                modifier = Modifier.size(24.dp),
                                tint = readerColors.content,
                            )
                        }
                        if (state.document is ReaderDocument.Epub) {
                            IconButton(onClick = { onAction(ReaderAction.OpenEpubSearch) }) {
                                Icon(Icons.Default.Search, contentDescription = "书内搜索", tint = readerColors.content)
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(state.showUI && state.sessionState is ReaderSessionState.Reading) {
                ReaderBottomBar(
                    progress = state.progress,
                    fontSizeSp = state.fontSizeSp,
                    readerTheme = state.theme,
                    currentChapterTitle = state.currentChapterTitle,
                    isSettingsPanelVisible = state.isSettingsPanelVisible,
                    onOpenSettings = { onAction(ReaderAction.OpenSettingsPanel) },
                    onCloseSettings = { onAction(ReaderAction.CloseSettingsPanel) },
                    onProgressChanged = { onAction(ReaderAction.SetProgress(it)) },
                    onFontSizeChanged = { onAction(ReaderAction.ChangeFontSize(it)) },
                    onThemeChanged = { onAction(ReaderAction.ChangeTheme(it)) },
                    ttsStatus = state.ttsPlayback.status,
                    ttsRate = state.ttsRate,
                    onToggleTts = { onAction(ReaderAction.ToggleTts) },
                    onStopTts = { onAction(ReaderAction.StopTts) },
                    onTtsRateChanged = { onAction(ReaderAction.ChangeTtsRate(it)) },
                )
            }
        }
    ) { innerPadding ->
        when (val session = state.sessionState) {
            ReaderSessionState.Idle -> ReaderEmptyState(
                modifier = Modifier.padding(innerPadding),
                onChooseFile = { onAction(ReaderAction.OpenFilePickerClick) },
            )
            ReaderSessionState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            is ReaderSessionState.Error -> ReaderErrorState(
                message = session.message,
                // 统一内容迁移后远端会话使用 content:*；旧 wenku8:* 仅为升级兼容键。
                isRemote = state.bookKey?.let { it.startsWith("content:") || it.startsWith("wenku8:") } == true,
                modifier = Modifier.padding(innerPadding),
                onRetry = { onAction(ReaderAction.RetryRemoteRead) },
                onChooseFile = { onAction(ReaderAction.OpenFilePickerClick) },
            )
            ReaderSessionState.Reading -> ReaderDocumentContent(
                state = state,
                modifier = Modifier.padding(innerPadding),
                onAction = onAction,
            )
        }
    }

    if (state.isBookmarkPanelVisible) {
        ReaderBookmarkSheet(
            bookmarks = state.bookmarks,
            onDismiss = { onAction(ReaderAction.CloseBookmarksPanel) },
            onJumpToBookmark = { onAction(ReaderAction.JumpToBookmark(it)) },
            onDeleteBookmark = { onAction(ReaderAction.DeleteBookmark(it)) },
        )
    }
    if (state.isEpubSearchVisible) {
        EpubSearchSheet(
            query = state.epubSearchQuery,
            results = state.epubSearchResults,
            onQuery = { onAction(ReaderAction.SearchEpub(it)) },
            onResult = { onAction(ReaderAction.JumpToEpubSearchResult(it)) },
            onDismiss = { onAction(ReaderAction.CloseEpubSearch) },
        )
    }
}

/** 阅读正文容器只负责分发不同格式的渲染参数，点击空白区域才切换沉浸式工具栏。 */
@Composable
private fun ReaderDocumentContent(
    state: ReaderUiState,
    modifier: Modifier,
    onAction: (ReaderAction) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().clickable { onAction(ReaderAction.ToggleUi) },
    ) {
        when (val document = state.document) {
            is ReaderDocument.Wenku8Full -> Wenku8FullReaderContent(
                fullText = document.text,
                chapters = document.chapters,
                targetChapterId = state.targetChapterId,
                initialProgress = state.readingPosition.progress,
                fontSizeSp = state.fontSizeSp,
                initialChapterId = state.readingPosition.chapterId,
                initialChapterIndex = state.readingPosition.chapterIndex,
                initialChapterProgress = state.readingPosition.chapterProgress,
                readerTheme = state.theme,
                restoreKey = state.positionRestoreRequest,
                onProgress = { onAction(ReaderAction.SetProgress(it)) },
                onReadingPosition = { onAction(ReaderAction.SetReadingPosition(it)) },
            )
            is ReaderDocument.Txt -> TxtReaderContent(
                fullText = document.text,
                fontSizeSp = state.fontSizeSp,
                initialProgress = state.readingPosition.progress,
                initialChapterId = state.readingPosition.chapterId,
                initialChapterIndex = state.readingPosition.chapterIndex,
                initialChapterProgress = state.readingPosition.chapterProgress,
                readerTheme = state.theme,
                restoreKey = state.positionRestoreRequest,
                onProgress = { onAction(ReaderAction.SetProgress(it)) },
                onReadingPosition = { onAction(ReaderAction.SetReadingPosition(it)) },
            )
            is ReaderDocument.Epub -> EpubReaderContent(
                epubChapters = document.chapters.map { it.content },
                fontSizeSp = state.fontSizeSp,
                initialProgress = state.readingPosition.progress,
                initialChapterIndex = state.readingPosition.chapterIndex,
                initialChapterProgress = state.readingPosition.chapterProgress,
                readerTheme = state.theme,
                restoreKey = state.positionRestoreRequest,
                onProgress = { onAction(ReaderAction.SetProgress(it)) },
                onReadingPosition = { onAction(ReaderAction.SetReadingPosition(it)) },
            )
            null -> Unit
        }
        state.remoteNotice?.let { notice ->
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = notice, color = MaterialTheme.colorScheme.tertiary)
                Button(onClick = { onAction(ReaderAction.RetryRemoteRead) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("重试连续阅读")
                }
            }
        }
    }
}

@Composable
private fun ReaderEmptyState(modifier: Modifier, onChooseFile: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("选择 TXT 或 EPUB 文件开始阅读", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onChooseFile, modifier = Modifier.padding(top = 16.dp)) { Text("选择文件") }
    }
}

@Composable
private fun ReaderErrorState(
    message: String,
    isRemote: Boolean,
    modifier: Modifier,
    onRetry: () -> Unit,
    onChooseFile: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        if (isRemote) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("重试") }
        } else {
            Button(onClick = onChooseFile, modifier = Modifier.padding(top = 16.dp)) { Text("重新选择文件") }
        }
    }
}

private data class ReaderScaffoldColors(val background: Color, val content: Color)

/** 顶栏、底栏和正文叠层使用相同主题色，避免仅正文变色造成割裂感。 */
@Composable
private fun readerScaffoldColors(theme: ReaderTheme): ReaderScaffoldColors = when (theme) {
    ReaderTheme.SYSTEM -> ReaderScaffoldColors(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground)
    ReaderTheme.LIGHT -> ReaderScaffoldColors(Color(0xFFFFFBFE), Color(0xFF1C1B1F))
    ReaderTheme.DARK -> ReaderScaffoldColors(Color(0xFF1C1B1F), Color(0xFFE6E1E5))
    ReaderTheme.SEPIA -> ReaderScaffoldColors(Color(0xFFF5ECD7), Color(0xFF5B4636))
}

@Preview(showBackground = true)
@Composable
fun TxtReaderPreview() {
    val fakeState = ReaderUiState(
        type = ReaderType.TXT,
        document = ReaderDocument.Txt(
            title = "测试书籍",
            text = buildString {
                appendLine("第一章 雨夜")
                appendLine()
                repeat(36) { index ->
                    appendLine("第 ${index + 1} 段：这是用于预览的阅读文本，验证正文排版、滚动和底部进度显示。")
                }
            }
        ),
        progress = 0.28f,
        showUI = true,
        fontSizeSp = 18f,
        sessionState = ReaderSessionState.Reading,
    )
    SakuyaInAndroidTheme(true) {
        ReaderContent(
            state = fakeState,
            bookTitle = "测试书籍",
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EpubReaderPreview() {
    val fakeState = ReaderUiState(
        type = ReaderType.EPUB,
        document = ReaderDocument.Epub(
            title = "测试 EPUB",
            chapters = listOf(
                ReaderChapter(
                    index = 0,
                    title = "Chapter 1",
                    content = "<h1>Chapter 1</h1><p>Some content for the first chapter.</p><p>This preview checks EPUB HTML rendering inside the reader.</p>"
                ),
                ReaderChapter(
                    index = 1,
                    title = "Chapter 2",
                    content = "<h1>Chapter 2</h1><p>The story continues here with another paragraph for layout preview.</p>"
                )
            )
        ),
        progress = 0.5f,
        showUI = true,
        fontSizeSp = 18f,
        sessionState = ReaderSessionState.Reading,
    )
    SakuyaInAndroidTheme(true) {
        ReaderContent(
            state = fakeState,
            bookTitle = "测试 EPUB",
            onAction = {}
        )
    }
}
