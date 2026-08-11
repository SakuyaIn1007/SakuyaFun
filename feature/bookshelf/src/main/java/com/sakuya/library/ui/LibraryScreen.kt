package com.sakuya.library.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.library.model.LibraryItem
import com.sakuya.library.model.LibraryItemType
import com.sakuya.library.viewmodel.LibraryAction
import com.sakuya.library.viewmodel.LibraryEffect
import com.sakuya.library.viewmodel.LibraryViewModel
import com.sakuya.ui.component.MediumContentCard
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenReader: (bookId: String, filePath: String) -> Unit,
    showTopBar: Boolean = true,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val bookItems by viewModel.bookItems.collectAsState()
    val recentBookItems by viewModel.recentBookItems.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LibraryEffect.OpenReader -> onOpenReader(effect.bookId, effect.filePath)
                is LibraryEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    LibraryContent(
        selectedTab = selectedTab,
        isEditing = isEditing,
        bookItems = bookItems,
        recentBookItems = recentBookItems,
        selectedItemIds = selectedItemIds,
        showTopBar = showTopBar,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    selectedTab: Int,
    isEditing: Boolean,
    recentBookItems: List<LibraryItem>,
    bookItems: List<LibraryItem>,
    selectedItemIds: Set<String>,
    showTopBar: Boolean = true,
    onAction: (LibraryAction) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var pendingImportRequest by remember { mutableStateOf<LibraryImportRequest>(LibraryImportRequest.Txt) }
    var searchQuery by remember { mutableStateOf("") }
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
        topBar = {
            if (showTopBar) {
                LibraryTabBar(
                    selectedTab = selectedTab,
                    isEditing = isEditing,
                selectedCount = selectedItemIds.size,
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                    onImportBook = ::launchImport,
                    onAction = onAction
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
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
                        items = bookItems.filterByQuery(searchQuery),
                        isEditing = isEditing,
                        selectedItemIds = selectedItemIds,
                        onAction = onAction
                    )
                    1 -> LibraryContentList(
                        items = recentBookItems.filterByQuery(searchQuery),
                        isEditing = isEditing,
                        selectedItemIds = selectedItemIds,
                        onAction = onAction
                    )
                    2 -> LibraryContentList(
                        items = emptyList(),
                        isEditing = isEditing,
                        selectedItemIds = selectedItemIds,
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
    selectedCount: Int,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onImportBook: (LibraryImportRequest) -> Unit,
    onAction: (LibraryAction) -> Unit
) {
    val tabs = listOf("喜欢", "历史")
    var importMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 与下方书籍网格统一为更紧凑的左右留白，避免工具栏显得比内容区域更宽松。
                .padding(start = 8.dp, top = 2.dp, end = 4.dp, bottom = 6.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier.weight(1f)
                            .padding(horizontal = 2.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onAction(LibraryAction.SelectTab(index)) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
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
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibrarySearchField(
                    query = searchQuery,
                    onQueryChanged = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .padding(start = 4.dp, end = 2.dp)
                )
                Spacer(Modifier.weight(1f))
                if (isEditing) {
                    IconButton(onClick = { onAction(LibraryAction.ToggleEdit) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, "取消")
                    }
                    IconButton(
                        onClick = { onAction(LibraryAction.DeleteSelectedItems) },
                        enabled = selectedCount > 0,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Delete, "删除选中书籍",
                            tint = if (selectedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                    }
                }
                Box {
                    IconButton(onClick = { importMenuExpanded = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Add, "导入外源小说")
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
}

@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .height(40.dp)
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
    ) { innerTextField ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(6.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("搜索", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                }
                innerTextField()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryContentList(
    items: List<LibraryItem>,
    isEditing: Boolean,
    selectedItemIds: Set<String>,
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

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 12.dp,
            end = 12.dp,
            bottom = 20.dp
        )
    ) {
        items(items, key = { it.id }) { item ->
            val isSelected = item.id in selectedItemIds
            Box(
                modifier = Modifier.combinedClickable(
                    onClick = {
                        if (isEditing) onAction(LibraryAction.ToggleItemSelected(item.id))
                        else onAction(LibraryAction.OpenBook(item))
                    },
                    onLongClick = {
                        if (!isEditing) onAction(LibraryAction.StartEditingWithItem(item.id))
                    }
                )
            ) {
                MediumContentCard(
                    title = item.title,
                    progress = item.progress,
                    fileType = item.fileTypeLabel(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isEditing) {
                    SelectionCircle(
                        selected = isSelected,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionCircle(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun List<LibraryItem>.filterByQuery(query: String): List<LibraryItem> {
    if (query.isBlank()) return this
    return filter { it.title.contains(query.trim(), ignoreCase = true) }
}

private fun LibraryItem.fileTypeLabel(): String = when(type) {
    LibraryItemType.EPUB -> "epub"
    LibraryItemType.TXT  -> "txt"
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
            recentBookItems = listOf(
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
            selectedItemIds = emptySet(),
            onAction = {}
        )
    }
}
