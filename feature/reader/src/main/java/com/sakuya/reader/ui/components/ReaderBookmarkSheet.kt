package com.sakuya.reader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sakuya.reader.model.ReaderBookmark

/**
 * ReaderBookmarkSheet.kt
 * 职责说明：以只读列表展示当前书籍的书签，并将跳转、删除和关闭意图回传给阅读器状态层。
 * 执行流程：ReaderUiState 提供书签快照 -> 用户点按书签/删除按钮 -> ReaderScreen 分发对应 ReaderAction
 * -> Room Flow 更新书签列表后，本面板自动重组；组件本身不访问数据库。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderBookmarkSheet(
    bookmarks: List<ReaderBookmark>,
    onDismiss: () -> Unit,
    onJumpToBookmark: (ReaderBookmark) -> Unit,
    onDeleteBookmark: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "书签",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            if (bookmarks.isEmpty()) {
                Text(
                    text = "还没有书签，阅读时可在顶部添加当前位置。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    items(items = bookmarks, key = ReaderBookmark::id) { bookmark ->
                        ReaderBookmarkItem(
                            bookmark = bookmark,
                            onClick = { onJumpToBookmark(bookmark) },
                            onDelete = { onDeleteBookmark(bookmark.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderBookmarkItem(
    bookmark: ReaderBookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Column {
        ListItem(
            headlineContent = {
                Text(bookmark.title.ifBlank { "阅读位置 ${(bookmark.progress * 100).toInt()}%" })
            },
            supportingContent = {
                val position = "进度 ${(bookmark.progress * 100).toInt()}%"
                val chapter = bookmark.chapterId?.takeIf { it.isNotBlank() }
                Text(if (chapter == null) position else "$chapter · $position")
            },
            trailingContent = {
                TextButton(onClick = onDelete) {
                    Text("删除")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        )
        HorizontalDivider()
    }
}
