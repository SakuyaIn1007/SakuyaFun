package com.sakuya.reader.ui.subpages

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakuya.reader.model.Wenku8ChapterAnchor
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * Wenku8FullReaderContent.kt
 * 职责说明：把会话内的 Wenku8 全文按章节锚点拆成连续滚动段落，并负责目录章节的精确定位。
 * 执行流程：Repository 过滤可靠锚点 -> 组件切割正文 -> LazyColumn 先定位 targetChapterId -> 滚动进度回传 ViewModel。
 */
@Composable
fun Wenku8FullReaderContent(
    fullText: String,
    chapters: List<Wenku8ChapterAnchor>,
    targetChapterId: String?,
    initialProgress: Float,
    fontSizeSp: Float,
    onProgress: (Float) -> Unit,
) {
    val sections = remember(fullText, chapters) { sections(fullText, chapters) }
    if (sections.isEmpty()) return

    val listState = rememberLazyListState()
    val textStyle = remember(fontSizeSp) {
        TextStyle(fontSize = fontSizeSp.sp, lineHeight = (fontSizeSp * 1.6f).sp)
    }

    LaunchedEffect(fullText, targetChapterId) {
        // LazyColumn 首次组合前尚无 item，等待布局后再滚动，确保章节标题而非正文中部成为起点。
        snapshotFlow { listState.layoutInfo.totalItemsCount }.filter { it > 0 }.first()
        val targetIndex = sections.indexOfFirst { it.chapterId == targetChapterId }
        val restoredIndex = (sections.lastIndex * initialProgress.coerceIn(0f, 1f)).roundToInt()
        listState.scrollToItem(if (targetIndex >= 0) targetIndex else restoredIndex)
    }

    LaunchedEffect(listState, sections.size) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                onProgress(if (sections.size <= 1) 0f else index.toFloat() / sections.lastIndex)
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
    ) {
        itemsIndexed(sections, key = { _, section -> section.chapterId }) { _, section ->
            Text(
                text = listOf(section.volumeTitle, section.title).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 10.dp),
            )
            Text(
                text = section.content,
                style = textStyle.copy(color = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** 章节内容从标题行之后开始，避免全文 TXT 中的原始标题与渲染标题重复显示。 */
private fun sections(fullText: String, anchors: List<Wenku8ChapterAnchor>): List<FullChapterSection> {
    val ordered = anchors.filter { it.offset in fullText.indices }.sortedBy { it.offset }
    return ordered.mapIndexed { index, anchor ->
        val end = ordered.getOrNull(index + 1)?.offset ?: fullText.length
        val raw = fullText.substring(anchor.offset, end)
        FullChapterSection(
            chapterId = anchor.chapterId,
            title = anchor.title,
            volumeTitle = anchor.volumeTitle,
            content = raw.substringAfter('\n', "").trim(),
        )
    }
}

private data class FullChapterSection(
    val chapterId: String,
    val title: String,
    val volumeTitle: String,
    val content: String,
)
