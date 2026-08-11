package com.sakuya.bookdetail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.bookdetail.viewmodel.BookDetailUiState
import com.sakuya.bookdetail.viewmodel.BookDetailViewModel
import com.sakuya.library.model.LibraryItem
import com.sakuya.library.model.LibraryItemType
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun BookDetailScreen(
    bookId: String,
    onBack: () -> Unit,
    onStartReading: (bookId: String, filePath: String) -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    BookDetailContent(
        uiState = uiState,
        onBack = onBack,
        onAddToLibrary = viewModel::addToLibrary,
        onStartReading = onStartReading
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookDetailContent(
    uiState: BookDetailUiState,
    onBack: () -> Unit,
    onAddToLibrary: () -> Unit,
    onStartReading: (bookId: String, filePath: String) -> Unit
) {
    val book = uiState.book
    val catalogBook = uiState.catalogBook
    val title = book?.title ?: catalogBook?.title ?: "未知书籍"
    val subtitle = book?.subtitle ?: catalogBook?.subtitle ?: "未找到书籍信息"
    val tags = book?.tags ?: catalogBook?.tags.orEmpty()
    val canRead = book?.isValid() == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书籍详情") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.ifEmpty { listOf("暂无分类") }.forEach { tag ->
                            AssistChip(
                                onClick = {},
                                label = { Text(tag) }
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Text(
                    text = when {
                        book?.isValid() == true -> "阅读进度 ${(book.progress * 100).toInt()}% · 收藏时间 ${book.collectedAt}"
                        book != null -> "已加入书架。导入与书名同名的 TXT 或 EPUB 后，会自动关联并可以开始阅读。"
                        catalogBook != null -> catalogBook.description
                        else -> "无法找到 ID 为 ${uiState.bookId} 的书籍信息。"
                    },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onAddToLibrary,
                    enabled = book == null && catalogBook != null && !uiState.isAddingToLibrary,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isAddingToLibrary) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (book == null) "加入书架" else "已在书架")
                    }
                }
                Button(
                    onClick = {
                        if (book != null) {
                            onStartReading(book.id, book.filePath)
                        }
                    },
                    enabled = canRead,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("开始阅读")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookDetailPreview() {
    SakuyaInAndroidTheme(true) {
        BookDetailContent(
            uiState = BookDetailUiState(
                bookId = "preview_1",
                book = LibraryItem(
                    id = "preview_1",
                    title = "本地书籍",
                    subtitle = "本地 TXT",
                    rating = 0f,
                    tags = listOf("本地导入"),
                    type = LibraryItemType.TXT,
                    collectedAt = "2026-07-07 12:00",
                    filePath = "/tmp/book.txt",
                    progress = 0.42f
                ),
                isLoading = false
            ),
            onBack = {},
            onAddToLibrary = {},
            onStartReading = { _, _ -> }
        )
    }
}
