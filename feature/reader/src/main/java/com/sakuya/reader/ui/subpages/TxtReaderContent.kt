package com.sakuya.reader.ui.subpages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakuya.reader.model.ReaderReadingPosition
import com.sakuya.reader.model.ReaderTheme
import com.sakuya.reader.model.ReaderType
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * TxtReaderContent.kt
 * 职责说明：以按段落分块的 LazyColumn 呈现 TXT 正文，并把可恢复的全局阅读比例回传上层。
 * 执行流程：原始文本先切成有限大小的正文块 -> LazyColumn 只测量可见块 ->
 * 首屏按已保存比例定位 -> 滚动快照同时回传兼容进度和统一阅读位置。
 */
@Composable
fun TxtReaderContent(
    fullText: String,
    fontSizeSp: Float,
    initialProgress: Float,
    onProgress: (Float) -> Unit,
    initialChapterId: String? = null,
    initialChapterIndex: Int = 0,
    initialChapterProgress: Float = initialProgress,
    restoreKey: Long = 0L,
    readerTheme: ReaderTheme = ReaderTheme.SYSTEM,
    onReadingPosition: (ReaderReadingPosition) -> Unit = {},
) {
    if (fullText.isEmpty()) return

    val blocks = remember(fullText) { splitTxtIntoBlocks(fullText) }
    val textStyle = remember(fontSizeSp) {
        TextStyle(
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.6f).sp
        )
    }
    val palette = readerContentPalette(readerTheme)
    val listState = rememberLazyListState()
    val currentOnProgress = rememberUpdatedState(onProgress)
    val currentOnReadingPosition = rememberUpdatedState(onReadingPosition)
    val restoredProgress = remember(fullText, restoreKey) {
        initialProgress.coerceIn(0f, 1f)
    }

    // TXT 没有可靠的章节边界，章节字段保持 0/空，仅把比例作为跨字体大小的恢复依据。
    @Suppress("UNUSED_VARIABLE")
    val ignoredChapterRestore = initialChapterId to initialChapterIndex to initialChapterProgress

    LaunchedEffect(fullText, restoreKey) {
        // 等待 LazyColumn 生成 item 后再定位；restoreKey 只由新会话或书签跳转改变。
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .filter { it > 0 }
            .first()
        val restoredIndex = ((blocks.size - 1) * restoredProgress).roundToInt()
        listState.scrollToItem(restoredIndex.coerceIn(0, blocks.lastIndex))
    }

    LaunchedEffect(listState, blocks.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val firstItem = layoutInfo.visibleItemsInfo.firstOrNull()
            TxtViewportProgress(
                firstIndex = firstItem?.index ?: 0,
                firstOffset = firstItem?.offset ?: 0,
                firstSize = firstItem?.size ?: 0,
                canScrollBackward = listState.canScrollBackward,
                canScrollForward = listState.canScrollForward,
            )
        }.distinctUntilChanged()
            .collect { viewport ->
                val progress = viewport.toProgress(blocks.size)
                currentOnProgress.value(progress)
                currentOnReadingPosition.value(
                    ReaderReadingPosition(
                        type = ReaderType.TXT,
                        progress = progress,
                        chapterProgress = progress,
                    )
                )
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        itemsIndexed(blocks, key = { index, _ -> index }) { _, block ->
            Text(
                text = block,
                style = textStyle.copy(color = palette.content),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
        }
    }
}

/**
 * 将长 TXT 以空行优先、换行次之的方式切块。分块只改变 Compose 的测量边界，保留原始字符顺序，
 * 因此不会影响复制文本和阅读内容；单个超长段落也会被硬性拆分，防止一个 Text 重新变成性能瓶颈。
 */
internal fun splitTxtIntoBlocks(
    fullText: String,
    maxBlockCharacters: Int = DEFAULT_TXT_BLOCK_CHARACTERS,
): List<String> {
    require(maxBlockCharacters > 0) { "maxBlockCharacters must be greater than zero" }
    if (fullText.isEmpty()) return emptyList()

    val blocks = mutableListOf<String>()
    val pending = StringBuilder()
    val normalizedText = fullText.replace("\r\n", "\n")

    splitByParagraphBoundary(normalizedText).forEach { paragraph ->
        var remaining = paragraph
        while (remaining.length > maxBlockCharacters) {
            if (pending.isNotEmpty()) {
                blocks += pending.toString()
                pending.clear()
            }
            val splitAt = naturalSplitIndex(remaining, maxBlockCharacters)
            blocks += remaining.substring(0, splitAt)
            remaining = remaining.substring(splitAt)
        }

        if (pending.length + remaining.length > maxBlockCharacters && pending.isNotEmpty()) {
            blocks += pending.toString()
            pending.clear()
        }
        pending.append(remaining)
    }

    if (pending.isNotEmpty()) {
        blocks += pending.toString()
    }
    return blocks.ifEmpty { listOf(normalizedText) }
}

/** 将两个及以上换行作为自然段边界，同时把边界字符保留在原始内容中。 */
private fun splitByParagraphBoundary(text: String): List<String> {
    val result = mutableListOf<String>()
    val boundary = Regex("\\n{2,}")
    var start = 0
    boundary.findAll(text).forEach { match ->
        result += text.substring(start, match.range.last + 1)
        start = match.range.last + 1
    }
    if (start < text.length) {
        result += text.substring(start)
    }
    return result.ifEmpty { listOf(text) }
}

/** 优先在换行或空白处切分，保证超长段落也不会交给单个 Text 测量。 */
private fun naturalSplitIndex(text: String, maxCharacters: Int): Int {
    val requestedEnd = maxCharacters.coerceAtMost(text.length - 1)
    val newline = text.lastIndexOf('\n', requestedEnd)
    if (newline > 0) return newline + 1

    val whitespace = text.lastIndexOf(' ', requestedEnd)
    if (whitespace > 0) return whitespace + 1

    return maxCharacters.coerceAtMost(text.length)
}

/** LazyList 只提供可见 item 的像素信息，此快照用于换算稳定的近似全局比例。 */
private data class TxtViewportProgress(
    val firstIndex: Int,
    val firstOffset: Int,
    val firstSize: Int,
    val canScrollBackward: Boolean,
    val canScrollForward: Boolean,
) {
    fun toProgress(blockCount: Int): Float {
        if (blockCount <= 0 || !canScrollBackward) return 0f
        if (!canScrollForward) return 1f

        val withinBlock = if (firstSize > 0) {
            (-firstOffset).toFloat().div(firstSize).coerceIn(0f, 1f)
        } else {
            0f
        }
        return ((firstIndex.coerceIn(0, blockCount - 1) + withinBlock) / blockCount)
            .coerceIn(0f, 1f)
    }
}

private const val DEFAULT_TXT_BLOCK_CHARACTERS = 3_000

@Preview(showBackground = true)
@Composable
fun TxtReaderPreview() {
    val sample = buildString {
        for (i in 1..100) {
            appendLine("第 $i 行：这是测试文本内容，用于验证滚动阅读模式加进度条。")
        }
    }
    SakuyaInAndroidTheme(true) {
        TxtReaderContent(
            fullText = sample,
            fontSizeSp = 18f,
            initialProgress = 0f,
            onProgress = {}
        )
    }
}
