package com.sakuya.library.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.library.model.LibraryItem
import com.sakuya.library.model.LibraryItemType
import com.sakuya.library.viewmodel.LibraryAction
import com.sakuya.library.viewmodel.LibraryEvent
import com.sakuya.library.viewmodel.LibraryViewModel
import com.sakuya.ui.component.MediumContentCard
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenReader: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val bookItems by viewModel.bookItems.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.OpenReader -> onOpenReader(event.filePath)
            }
        }
    }
    LibraryContent(
        selectedTab = selectedTab,
        isEditing = isEditing,
        bookItems = bookItems,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    selectedTab: Int,
    isEditing: Boolean,
    bookItems: List<LibraryItem>,
    onAction: (LibraryAction) -> Unit
) {
    val context = LocalContext.current
    var pendingImportRequest by remember { mutableStateOf<LibraryImportRequest>(LibraryImportRequest.Txt) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val title = resolveDisplayName(context, uri)
            val type = resolveImportType(
                context = context,
                uri = uri,
                title = title,
                request = pendingImportRequest
            )
            persistReadPermission(context, uri)
            onAction(
                LibraryAction.ImportBook(
                    filePath = uri.toString(),
                    title = title,
                    type = type
                )
            )
        }
    }

    fun launchImport(request: LibraryImportRequest) {
        pendingImportRequest = request
        filePickerLauncher.launch(request.mimeTypes.toTypedArray())
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            LibraryTabBar(
                selectedTab = selectedTab,
                isEditing = isEditing,
                onImportBook = ::launchImport,
                onAction = onAction
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { it / 4 }) togetherWith
                    (fadeOut() + slideOutHorizontally { -it / 4 })
                }
            ) { tab ->
                when (tab) {
                    0 -> LibraryContentList(
                        items = bookItems,
                        isEditing = isEditing,
                        onAction = onAction
                    )
                    1 -> LibraryContentList(
                        items = bookItems,
                        isEditing = isEditing,
                        onAction = onAction
                    )
                    2 -> LibraryContentList(
                        items = emptyList(),
                        isEditing = isEditing,
                        onAction = onAction
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryTabBar(
    selectedTab: Int,
    isEditing: Boolean,
    onImportBook: (LibraryImportRequest) -> Unit,
    onAction: (LibraryAction) -> Unit
) {
    val tabs = listOf("书架", "历史", "收藏")
    var importMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 10.dp, end = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .clickable { onAction(LibraryAction.SelectTab(index)) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected)
                            FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(3.dp)
                            .background(
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onAction(LibraryAction.ToggleEdit) }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑书架",
                    tint = if (isEditing)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { importMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "导入书籍",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = importMenuExpanded,
                    onDismissRequest = { importMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("导入 TXT") },
                        onClick = {
                            importMenuExpanded = false
                            onImportBook(LibraryImportRequest.Txt)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("导入 EPUB") },
                        onClick = {
                            importMenuExpanded = false
                            onImportBook(LibraryImportRequest.Epub)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("从本地文件选择") },
                        onClick = {
                            importMenuExpanded = false
                            onImportBook(LibraryImportRequest.AnySupportedBook)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryContentList(
    items: List<LibraryItem>,
    isEditing: Boolean,
    onAction: (LibraryAction) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无收藏内容",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    val sortedItems = items.sortedByDescending { it.collectedAt }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 20.dp
        )
    ) {
        items(sortedItems, key = { it.id }) { item ->
            Box(
                modifier = Modifier.clickable(enabled = !isEditing) {
                    onAction(LibraryAction.OpenBook(item))
                }
            ) {
                MediumContentCard(
                    title = item.title,
                    progress = item.rating / 5f,
                    fileType = item.fileTypeLabel(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isEditing) {
                    IconButton(
                        onClick = { onAction(LibraryAction.RemoveItem(item.id)) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "取消收藏",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

private fun LibraryItem.fileTypeLabel(): String = when {
    filePath.endsWith(".epub", ignoreCase = true) -> "epub"
    filePath.endsWith(".txt", ignoreCase = true) -> "txt"
    type == LibraryItemType.EPUB -> "epub"
    else -> "txt"
}

private sealed class LibraryImportRequest(
    val mimeTypes: List<String>
) {
    data object Txt : LibraryImportRequest(
        mimeTypes = listOf("text/plain")
    )

    data object Epub : LibraryImportRequest(
        mimeTypes = listOf("application/epub+zip")
    )

    data object AnySupportedBook : LibraryImportRequest(
        mimeTypes = listOf("text/plain", "application/epub+zip")
    )
}

private fun resolveImportType(
    context: Context,
    uri: Uri,
    title: String,
    request: LibraryImportRequest
): LibraryItemType {
    return when (request) {
        LibraryImportRequest.Txt -> LibraryItemType.TXT
        LibraryImportRequest.Epub -> LibraryItemType.EPUB
        LibraryImportRequest.AnySupportedBook -> {
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            when {
                mimeType == "application/epub+zip" -> LibraryItemType.EPUB
                title.endsWith(".epub", ignoreCase = true) -> LibraryItemType.EPUB
                else -> LibraryItemType.TXT
            }
        }
    }
}

private fun persistReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

private fun resolveDisplayName(context: Context, uri: Uri): String {
    val displayName = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    }.getOrNull()

    return displayName
        ?: uri.lastPathSegment?.substringAfterLast("/")
        ?: "未命名书籍"
}

@Preview(showBackground = true)
@Composable
private fun LibraryContentPreview() {
    SakuyaInAndroidTheme(true) {
        LibraryContent(
            selectedTab = 0,
            isEditing = false,
            bookItems = listOf(
                LibraryItem(
                    id = "novel_1",
                    title = "刀剑神域",
                    subtitle = "川原砾 · 电击文库",
                    rating = 4.8f,
                    tags = listOf("科幻", "冒险"),
                    type = LibraryItemType.TXT,
                    collectedAt = "2026-06-01"
                ),
            ),
            onAction = {}
        )
    }
}
