package com.sakuya.reader.ui.subpages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.sakuya.reader.model.ReaderTheme
import java.util.Locale

/**
 * ReaderContentTheme.kt
 * 职责说明：为正文渲染组件提供统一的阅读背景和文字颜色。
 * 执行流程：ReaderScreen 传入用户主题偏好 -> 本类映射成正文色板 -> TXT、Wenku8 与 EPUB
 * 使用同一组颜色渲染；SYSTEM 则始终跟随外层 MaterialTheme，避免重复维护系统深浅色逻辑。
 */
internal data class ReaderContentPalette(
    val background: Color,
    val content: Color,
)

/**
 * 主题色板只存在于 UI 层，不能被持久化层或正文解析层依赖。
 * 固定主题的颜色同时用于原生 Compose 文本和 EPUB CSS，确保切换主题时视觉一致。
 */
@Composable
internal fun readerContentPalette(readerTheme: ReaderTheme): ReaderContentPalette = when (readerTheme) {
    ReaderTheme.SYSTEM -> ReaderContentPalette(
        background = MaterialTheme.colorScheme.background,
        content = MaterialTheme.colorScheme.onBackground,
    )

    ReaderTheme.LIGHT -> ReaderContentPalette(
        background = Color(0xFFFFFBFE),
        content = Color(0xFF1C1B1F),
    )

    ReaderTheme.DARK -> ReaderContentPalette(
        background = Color(0xFF1C1B1F),
        content = Color(0xFFE6E1E5),
    )

    ReaderTheme.SEPIA -> ReaderContentPalette(
        background = Color(0xFFF5ECD7),
        content = Color(0xFF5B4636),
    )
}

/** WebView CSS 不接受 Compose Color，统一转成不含透明度的 #RRGGBB 值。 */
internal fun Color.toReaderCssColor(): String = String.format(
    Locale.US,
    "#%06X",
    toArgb() and 0x00FFFFFF,
)
