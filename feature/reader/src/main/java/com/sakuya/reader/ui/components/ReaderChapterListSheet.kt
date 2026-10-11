package com.sakuya.reader.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.reader.model.ReaderTocEntry
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import com.sakuya.ui.theme.SakuyaTheme

/**
 * ReaderChapterListSheet.kt
 * 职责说明：以只读列表展示当前书籍的目录，并把跳章意图回传给阅读器状态层。
 * 执行流程：ReaderUiState.document 构建 ReaderTocEntry 快照 -> 用户点按章节 -> ReaderScreen 分发
 * ReaderAction.JumpToChapter -> ViewModel 换算位置并触发一次正文定位；组件本身不访问文档数据。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderChapterListSheet(
    entries: List<ReaderTocEntry>,
    onDismiss: () -> Unit,
    onJump: (ReaderTocEntry) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ReaderChapterListContent(entries = entries, onJump = onJump)
    }
}

/** 面板内容独立出来，便于在 Preview 中脱离 ModalBottomSheet 宿主渲染。 */
@Composable
fun ReaderChapterListContent(
    entries: List<ReaderTocEntry>,
    onJump: (ReaderTocEntry) -> Unit,
) {
    val dimensions = SakuyaTheme.tokens.dimensions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensions.spaceXl),
    ) {
        Text(
            text = "目录",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = dimensions.spaceXl, vertical = dimensions.spaceSm),
        )

        if (entries.isEmpty()) {
            Text(
                text = "本书没有可跳转的章节。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = dimensions.spaceXl, vertical = 20.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 480.dp),
            ) {
                items(items = entries, key = ReaderTocEntry::key) { entry ->
                    ReaderChapterItem(entry = entry, onClick = { onJump(entry) })
                }
            }
        }
    }
}

@Composable
private fun ReaderChapterItem(
    entry: ReaderTocEntry,
    onClick: () -> Unit,
) {
    Column {
        ListItem(
            headlineContent = {
                Text(entry.title.ifBlank { "未命名章节" })
            },
            supportingContent = entry.subtitle?.let { subtitle -> { Text(subtitle) } },
            trailingContent = if (entry.isCurrent) {
                { Text("当前", color = MaterialTheme.colorScheme.primary) }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        )
        HorizontalDivider()
    }
}

@Preview(showBackground = true)
@Composable
private fun ReaderChapterListContentPreview() {
    SakuyaInAndroidTheme(true) {
        ReaderChapterListContent(
            entries = listOf(
                ReaderTocEntry(title = "第1章 雨夜", subtitle = "第一卷 起始", chapterId = "c1", chapterIndex = 0),
                ReaderTocEntry(title = "第2章 相遇", subtitle = "第一卷 起始", chapterId = "c2", chapterIndex = 1, isCurrent = true),
                ReaderTocEntry(title = "第3章 离别", subtitle = "第二卷 转折", chapterId = "c3", chapterIndex = 2),
            ),
            onJump = {},
        )
    }
}
