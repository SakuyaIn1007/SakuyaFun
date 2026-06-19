package com.sakuya.reader.ui

import android.R
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakuya.reader.model.ReaderChapter
import com.sakuya.reader.model.ReaderDocument
import com.sakuya.reader.model.ReaderType
import com.sakuya.reader.ui.components.ReaderBottomBar
import com.sakuya.reader.ui.components.ReaderTopBar
import com.sakuya.reader.ui.subpages.EpubReaderContent
import com.sakuya.reader.ui.subpages.TxtReaderContent
import com.sakuya.reader.viewmodel.ReaderUiState
import com.sakuya.reader.viewmodel.ReaderViewModel
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import java.io.File

@Composable
fun ReaderScreen(
    filePath: String,
    onBack: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var currentFileLabel by remember { mutableStateOf(if (filePath.isNotEmpty()) File(filePath).name else "") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            currentFileLabel = uri.lastPathSegment ?: ""
            viewModel.openFile(uri)
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
            viewModel.openFile(fileUri)
        }
    }

    ReaderContent(
        state = state,
        bookTitle = state.document?.title ?: currentFileLabel,
        onBack = onBack,
        onProgress = viewModel::setProgress,
        onToggleUI = viewModel::toggleUI,
        onFontSizeChanged = viewModel::changeFontSize,
        onOpenFile = {
            filePickerLauncher.launch(
                arrayOf(
                    "application/epub+zip",
                    "text/plain",
                    "*/*"
                )
            )
        }
    )
}

@Composable
fun ReaderContent(
    state: ReaderUiState,
    bookTitle: String = "",
    onBack: () -> Unit = {},
    onProgress: (Float) -> Unit = {},
    onToggleUI: () -> Unit,
    onFontSizeChanged: (Float) -> Unit = {},
    onOpenFile: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            AnimatedVisibility(state.showUI && !state.isLoading) {
                ReaderTopBar(
                    title = bookTitle,
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = onOpenFile) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "选择文件",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(state.showUI && !state.isLoading) {
                ReaderBottomBar(
                    progress = state.progress,
                    fontSizeSp = state.fontSizeSp,
                    onFontSizeChanged = onFontSizeChanged
                )
            }
        }
    ) { innerPadding ->

        /* reader的三种状态
        *  1. 加载状态
        *  2. 显示错误
        *  3. 正常阅读
        * */
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .clickable {
                        onToggleUI()
                    }
            ) {
                when (val document = state.document) {
                    is ReaderDocument.Txt -> TxtReaderContent(
                        fullText = document.text,
                        fontSizeSp = state.fontSizeSp,
                        onProgress = onProgress
                    )
                    is ReaderDocument.Epub -> EpubReaderContent(
                        epubChapters = document.chapters.map { it.content },
                        fontSizeSp = state.fontSizeSp,
                        onProgress = onProgress
                    )
                    null -> Unit
                }
            }
        }
    }
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
        fontSizeSp = 18f
    )
    SakuyaInAndroidTheme(true) {
        ReaderContent(
            state = fakeState,
            bookTitle = "测试书籍",
            onToggleUI = {}
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
        fontSizeSp = 18f
    )
    SakuyaInAndroidTheme(true) {
        ReaderContent(
            state = fakeState,
            bookTitle = "测试 EPUB",
            onToggleUI = {}
        )
    }
}
