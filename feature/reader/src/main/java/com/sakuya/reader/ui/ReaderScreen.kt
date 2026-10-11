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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.sakuya.reader.model.buildTocEntries
import com.sakuya.reader.ui.components.ReaderBottomBar
import com.sakuya.reader.ui.components.ReaderBookmarkSheet
import com.sakuya.reader.ui.components.ReaderChapterListSheet
import com.sakuya.reader.ui.components.ReaderMenuItem
import com.sakuya.reader.ui.components.ReaderOverflowMenu
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
import com.sakuya.ui.theme.SakuyaTheme
import java.io.File

@Composable
fun ReaderScreen(
    bookId: String?,
    filePath: String,
    /** 第二项为 null 表示「整本阅读」：从头连续阅读，无需定位目标章。 */
    remoteChapter: Triple<String, String?, String>? = null,
    onBack: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
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
        onBack = onBack,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun ReaderContent(
    state: ReaderUiState,
    onBack: () -> Unit = {},
    onAction: (ReaderAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val readerColors = readerScaffoldColors(state.theme)
    var isMenuExpanded by remember { mutableStateOf(false) }

    /**
     * 次要操作全部收进「更多」菜单，顶栏只保留返回与菜单入口；
     * 目录条目在此处按文档类型构建，目录与书内搜索分别只对带章节/EPUB 的会话出现。
     */
    val tocEntries = remember(
        state.document,
        state.readingPosition.chapterId,
        state.readingPosition.chapterIndex,
    ) {
        buildTocEntries(
            document = state.document,
            currentChapterId = state.readingPosition.chapterId,
            currentChapterIndex = state.readingPosition.chapterIndex,
        )
    }
    val isRemoteSession = state.bookKey?.startsWith("content:") == true
    val document = state.document

    Scaffold(
        containerColor = readerColors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AnimatedVisibility(state.showUI && state.sessionState !is ReaderSessionState.Loading) {
                ReaderTopBar(
                    onBack = onBack,
                    containerColor = readerColors.background,
                    contentColor = readerColors.content,
                    actions = {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多",
                                tint = readerColors.content,
                            )
                        }
                        ReaderOverflowMenu(
                            expanded = isMenuExpanded,
                            onDismiss = { isMenuExpanded = false },
                            containerColor = readerColors.background,
                            contentColor = readerColors.content,
                            items = readerMenuItems(
                                state = state,
                                document = document,
                                hasToc = tocEntries.isNotEmpty(),
                                isRemoteSession = isRemoteSession,
                                onAction = onAction,
                            ),
                        )
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
    if (state.isChapterListVisible) {
        ReaderChapterListSheet(
            entries = tocEntries,
            onDismiss = { onAction(ReaderAction.CloseChapterList) },
            onJump = { entry -> onAction(ReaderAction.JumpToChapter(entry.chapterId, entry.chapterIndex)) },
        )
    }
}

/**
 * 组装「更多」菜单项：正文操作恒显，同步与离线下载整簇仅在远端 content: 会话出现，
 * 状态文案与可点性直接映射 UiState，组件本身不做判断。
 */
private fun readerMenuItems(
    state: ReaderUiState,
    document: ReaderDocument?,
    hasToc: Boolean,
    isRemoteSession: Boolean,
    onAction: (ReaderAction) -> Unit,
): List<ReaderMenuItem> = buildList {
    add(ReaderMenuItem("切换本地文件", { onAction(ReaderAction.OpenFilePickerClick) }))
    add(ReaderMenuItem("添加书签", { onAction(ReaderAction.AddBookmark) }))
    add(ReaderMenuItem("查看书签", { onAction(ReaderAction.OpenBookmarksPanel) }))
    if (document is ReaderDocument.Epub) {
        add(ReaderMenuItem("书内搜索", { onAction(ReaderAction.OpenEpubSearch) }))
    }
    if (hasToc) {
        add(ReaderMenuItem("目录", { onAction(ReaderAction.OpenChapterList) }))
    }
    if (isRemoteSession) {
        add(
            ReaderMenuItem(
                text = "同步：" + when (state.syncStatus) {
                    ReadingSyncStatus.SYNCED -> "已同步"
                    ReadingSyncStatus.PENDING -> "待同步"
                    ReadingSyncStatus.SYNCING -> "同步中"
                    ReadingSyncStatus.FAILED -> "同步失败"
                },
                onClick = { onAction(ReaderAction.RetrySync) },
                enabled = state.syncStatus != ReadingSyncStatus.SYNCING,
                isError = state.syncStatus == ReadingSyncStatus.FAILED,
                showDividerAbove = true,
            )
        )
        val download = state.offlineDownload
        add(
            ReaderMenuItem(
                text = when (download?.status) {
                    OfflineDownloadStatus.DOWNLOADING -> "暂停下载 ${(download.progress * 100).toInt()}%"
                    OfflineDownloadStatus.PAUSED -> "继续下载 ${(download.progress * 100).toInt()}%"
                    OfflineDownloadStatus.FAILED -> "重试下载"
                    OfflineDownloadStatus.COMPLETED -> "离线内容 ${"%.1f".format(state.offlineStorageBytes / 1024f / 1024f)}M"
                    else -> "下载离线内容"
                },
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
                isError = download?.status == OfflineDownloadStatus.FAILED,
            )
        )
        // 钉住只对已完成的离线内容有意义：其余状态本就受淘汰规则保护，无钉住价值。
        if (download?.status == OfflineDownloadStatus.COMPLETED) {
            add(
                ReaderMenuItem(
                    text = if (download.pinned) "取消钉住" else "钉住（不自动清理）",
                    onClick = { onAction(ReaderAction.TogglePinnedOffline) },
                )
            )
        }
        if (download != null) {
            add(ReaderMenuItem("删除离线内容", { onAction(ReaderAction.DeleteOfflineDownload) }))
        }
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
                modifier = Modifier.align(Alignment.TopCenter).padding(SakuyaTheme.tokens.dimensions.spaceLg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = notice, color = MaterialTheme.colorScheme.tertiary)
                Button(onClick = { onAction(ReaderAction.RetryRemoteRead) }, modifier = Modifier.padding(top = SakuyaTheme.tokens.dimensions.spaceSm)) {
                    Text("重试连续阅读")
                }
            }
        }
    }
}

@Composable
private fun ReaderEmptyState(modifier: Modifier, onChooseFile: () -> Unit) {
    val dimensions = SakuyaTheme.tokens.dimensions
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("选择 TXT 或 EPUB 文件开始阅读", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onChooseFile, modifier = Modifier.padding(top = dimensions.spaceLg)) { Text("选择文件") }
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
    val dimensions = SakuyaTheme.tokens.dimensions
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        if (isRemote) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = dimensions.spaceLg)) { Text("重试") }
        } else {
            Button(onClick = onChooseFile, modifier = Modifier.padding(top = dimensions.spaceLg)) { Text("重新选择文件") }
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
            onAction = {}
        )
    }
}
