package com.sakuya.reader.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.reader.model.ReaderTheme
import com.sakuya.reader.tts.TtsPlaybackStatus
import com.sakuya.ui.motion.MotionVisibility

/**
 * ReaderBottomBar.kt
 * 职责说明：承载阅读进度和可展开的阅读设置，不直接持有或写入阅读会话数据。
 * 执行流程：ReaderScreen 将不可变 UiState 映射为展示参数 -> 用户调整进度、字号或主题
 * -> 回调 ReaderAction -> ViewModel 更新状态并由本组件重新渲染。
 */
@Composable
fun ReaderBottomBar(
    progress: Float,
    fontSizeSp: Float,
    readerTheme: ReaderTheme = ReaderTheme.SYSTEM,
    currentChapterTitle: String? = null,
    isSettingsPanelVisible: Boolean = false,
    onOpenSettings: () -> Unit = {},
    onCloseSettings: () -> Unit = {},
    onProgressChanged: (Float) -> Unit = {},
    onFontSizeChanged: (Float) -> Unit = {},
    onThemeChanged: (ReaderTheme) -> Unit = {},
    ttsStatus: TtsPlaybackStatus = TtsPlaybackStatus.IDLE,
    ttsRate: Float = 1f,
    onToggleTts: () -> Unit = {},
    onStopTts: () -> Unit = {},
    onTtsRateChanged: (Float) -> Unit = {},
) {
    ReaderBottomBarContent(
        progress = progress,
        fontSizeSp = fontSizeSp,
        readerTheme = readerTheme,
        currentChapterTitle = currentChapterTitle,
        isSettingsPanelVisible = isSettingsPanelVisible,
        onOpenSettings = onOpenSettings,
        onCloseSettings = onCloseSettings,
        onProgressChanged = onProgressChanged,
        onFontSizeChanged = onFontSizeChanged,
        onThemeChanged = onThemeChanged,
        ttsStatus = ttsStatus,
        ttsRate = ttsRate,
        onToggleTts = onToggleTts,
        onStopTts = onStopTts,
        onTtsRateChanged = onTtsRateChanged,
    )
}

@Composable
fun ReaderBottomBarContent(
    progress: Float,
    fontSizeSp: Float,
    readerTheme: ReaderTheme = ReaderTheme.SYSTEM,
    currentChapterTitle: String? = null,
    isSettingsPanelVisible: Boolean = false,
    onOpenSettings: () -> Unit = {},
    onCloseSettings: () -> Unit = {},
    onProgressChanged: (Float) -> Unit = {},
    onFontSizeChanged: (Float) -> Unit = {},
    onThemeChanged: (ReaderTheme) -> Unit = {},
    ttsStatus: TtsPlaybackStatus = TtsPlaybackStatus.IDLE,
    ttsRate: Float = 1f,
    onToggleTts: () -> Unit = {},
    onStopTts: () -> Unit = {},
    onTtsRateChanged: (Float) -> Unit = {},
) {
    val safeProgress = progress.coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${(safeProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 8.dp),
            )
            LinearProgressIndicator(
                progress = { safeProgress },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = if (isSettingsPanelVisible) onCloseSettings else onOpenSettings) {
                Text(if (isSettingsPanelVisible) "收起" else "阅读设置")
            }
        }

        MotionVisibility(visible = isSettingsPanelVisible) {
            ReaderSettingsPanel(
                progress = safeProgress,
                fontSizeSp = fontSizeSp,
                readerTheme = readerTheme,
                currentChapterTitle = currentChapterTitle,
                onProgressChanged = onProgressChanged,
                onFontSizeChanged = onFontSizeChanged,
                onThemeChanged = onThemeChanged,
                ttsStatus = ttsStatus,
                ttsRate = ttsRate,
                onToggleTts = onToggleTts,
                onStopTts = onStopTts,
                onTtsRateChanged = onTtsRateChanged,
                onClose = onCloseSettings,
            )
        }
    }
}

/**
 * 阅读设置面板只负责收集用户输入；进度 Slider 的连续回调由 ViewModel 防抖保存，
 * 因此拖动过程中 UI 可以立即跟随状态，又不会频繁写入 Room。
 */
@Composable
private fun ReaderSettingsPanel(
    progress: Float,
    fontSizeSp: Float,
    readerTheme: ReaderTheme,
    currentChapterTitle: String?,
    onProgressChanged: (Float) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onThemeChanged: (ReaderTheme) -> Unit,
    ttsStatus: TtsPlaybackStatus,
    ttsRate: Float,
    onToggleTts: () -> Unit,
    onStopTts: () -> Unit,
    onTtsRateChanged: (Float) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "阅读设置",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) {
                Text("完成")
            }
        }

        Text("朗读速度 ${"%.1f".format(ttsRate)}x", style = MaterialTheme.typography.labelLarge)
        Slider(value = ttsRate, onValueChange = onTtsRateChanged, valueRange = 0.5f..2f, steps = 5)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onToggleTts) {
                Text(if (ttsStatus == TtsPlaybackStatus.PLAYING || ttsStatus == TtsPlaybackStatus.PREPARING) "暂停朗读" else "开始朗读")
            }
            if (ttsStatus != TtsPlaybackStatus.IDLE) TextButton(onClick = onStopTts) { Text("停止") }
        }

        currentChapterTitle
            ?.takeIf { it.isNotBlank() }
            ?.let { title ->
                Text(
                    text = "当前位置：$title",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

        Text(
            text = "阅读进度 ${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = progress,
            onValueChange = onProgressChanged,
            valueRange = 0f..1f,
        )

        Text(
            text = "字号 ${fontSizeSp.toInt()}sp",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = fontSizeSp.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP),
            onValueChange = onFontSizeChanged,
            valueRange = MIN_FONT_SIZE_SP..MAX_FONT_SIZE_SP,
            steps = (MAX_FONT_SIZE_SP - MIN_FONT_SIZE_SP).toInt() - 1,
        )

        Text(
            text = "阅读主题",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReaderTheme.entries.forEach { theme ->
                FilterChip(
                    selected = theme == readerTheme,
                    onClick = { onThemeChanged(theme) },
                    label = { Text(theme.label) },
                )
            }
        }
    }
}

private const val MIN_FONT_SIZE_SP = 12f
private const val MAX_FONT_SIZE_SP = 32f

private val ReaderTheme.label: String
    get() = when (this) {
        ReaderTheme.SYSTEM -> "跟随系统"
        ReaderTheme.LIGHT -> "明亮"
        ReaderTheme.DARK -> "深色"
        ReaderTheme.SEPIA -> "护眼"
    }

@Preview(showBackground = true)
@Composable
private fun ReaderBottomBarPreview() {
    ReaderBottomBarContent(
        progress = 0.42f,
        fontSizeSp = 18f,
        readerTheme = ReaderTheme.SEPIA,
        currentChapterTitle = "第一章 雨夜",
        isSettingsPanelVisible = true,
    )
}
