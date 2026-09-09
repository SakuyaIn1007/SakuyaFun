package com.sakuya.reader.ui.subpages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakuya.reader.model.ReaderReadingPosition
import com.sakuya.reader.model.ReaderTheme
import com.sakuya.reader.model.ReaderType
import com.sakuya.reader.model.Wenku8ChapterAnchor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * Wenku8FullReaderContent.kt
 * 职责说明：将 Wenku8 全文按可信章节锚点拆分为连续 LazyColumn，并回传章节加章节内偏移。
 * 执行流程：目录选中章节优先于保存位置 -> LazyColumn 定位到对应 item -> 可见 item 的像素偏移
 * 换算全局和章节内比例 -> ViewModel 持久化，以避免只记录章节索引导致进度跳变。
 */
@Composable
fun Wenku8FullReaderContent(
    fullText: String,
    chapters: List<Wenku8ChapterAnchor>,
    targetChapterId: String?,
    initialProgress: Float,
    fontSizeSp: Float,
    onProgress: (Float) -> Unit,
    initialChapterId: String? = null,
    initialChapterIndex: Int = 0,
    initialChapterProgress: Float = initialProgress,
    readerTheme: ReaderTheme = ReaderTheme.SYSTEM,
    restoreKey: Long = 0L,
    onReadingPosition: (ReaderReadingPosition) -> Unit = {},
) {
    val sections = remember(fullText, chapters) { sections(fullText, chapters) }
    if (sections.isEmpty()) return

    val listState = rememberLazyListState()
    val palette = readerContentPalette(readerTheme)
    val textStyle = remember(fontSizeSp) {
        TextStyle(fontSize = fontSizeSp.sp, lineHeight = (fontSizeSp * 1.6f).sp)
    }
    val currentOnProgress = rememberUpdatedState(onProgress)
    val currentOnReadingPosition = rememberUpdatedState(onReadingPosition)

    LaunchedEffect(fullText, targetChapterId, restoreKey) {
        // LazyColumn 首次组合前尚无 item，等待布局后再滚动，确保章节标题而非正文中部成为起点。
        snapshotFlow { listState.layoutInfo.totalItemsCount }.filter { it > 0 }.first()
        val explicitTarget = sections.indexOfFirst { it.chapterId == targetChapterId }
        val savedTarget = sections.indexOfFirst { it.chapterId == initialChapterId }
        val byIndex = initialChapterIndex.takeIf { it in sections.indices }
        val fallback = (sections.lastIndex * initialProgress.coerceIn(0f, 1f)).roundToInt()
        val index = when {
            explicitTarget >= 0 -> explicitTarget
            savedTarget >= 0 -> savedTarget
            byIndex != null -> byIndex
            else -> fallback
        }.coerceIn(0, sections.lastIndex)
        val offset = if (explicitTarget >= 0) 0 else {
            val estimatedHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            -(estimatedHeight * initialChapterProgress.coerceIn(0f, 1f)).roundToInt()
        }
        listState.scrollToItem(index, offset)
    }

    LaunchedEffect(listState, sections.size) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val item = layout.visibleItemsInfo.firstOrNull()
            Wenku8ViewportPosition(
                index = item?.index ?: 0,
                offset = item?.offset ?: 0,
                size = item?.size ?: 0,
                canScrollBackward = listState.canScrollBackward,
                canScrollForward = listState.canScrollForward,
            )
        }.distinctUntilChanged().collect { viewport ->
            val position = viewport.toPosition(sections)
            currentOnProgress.value(position.progress)
            currentOnReadingPosition.value(position)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(palette.background),
        state = listState,
    ) {
        itemsIndexed(sections, key = { _, section -> section.chapterId }) { _, section ->
            Text(
                text = listOf(section.volumeTitle, section.title).filter { it.isNotBlank() }.joinToString(" · "),
                style = textStyle.copy(fontSize = (fontSizeSp + 3f).sp, color = palette.content),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 10.dp),
            )
            Text(
                text = section.content,
                style = textStyle.copy(color = palette.content),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** 只依赖首个可见章节的布局数据，避免观察整份长文本或为历史章节创建动画/状态。 */
private data class Wenku8ViewportPosition(
    val index: Int,
    val offset: Int,
    val size: Int,
    val canScrollBackward: Boolean,
    val canScrollForward: Boolean,
) {
    fun toPosition(sections: List<FullChapterSection>): ReaderReadingPosition {
        if (sections.isEmpty()) return ReaderReadingPosition(type = ReaderType.WENKU8)
        val safeIndex = index.coerceIn(0, sections.lastIndex)
        val local = if (size > 0) (-offset).toFloat().div(size).coerceIn(0f, 1f) else 0f
        val progress = when {
            !canScrollBackward -> 0f
            !canScrollForward -> 1f
            else -> (safeIndex + local) / sections.size
        }.coerceIn(0f, 1f)
        return ReaderReadingPosition(
            type = ReaderType.WENKU8,
            progress = progress,
            chapterId = sections[safeIndex].chapterId,
            chapterIndex = safeIndex,
            chapterProgress = local,
        )
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
