package com.sakuya.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * DesignTokens.kt
 * 职责说明：
 * 1. 承载 Material ColorScheme 无法表达的业务语义色和统一尺寸规范。
 * 2. 由 SakuyaInAndroidTheme 在应用根部注入，Composable 通过 SakuyaTheme.tokens 读取。
 * 3. 仅用于界面层级和状态表达，头像、封面等业务内容颜色不应使用这些令牌替换。
 */
data class SakuyaColorTokens(
    val elevatedSurface: Color,
    val pressedSurface: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
)

data class SakuyaDimensionTokens(
    val spaceXs: Dp = 4.dp,
    val spaceSm: Dp = 8.dp,
    val spaceMd: Dp = 12.dp,
    val spaceLg: Dp = 16.dp,
    val spaceXl: Dp = 24.dp,
    val radiusSmall: Dp = 10.dp,
    val radiusMedium: Dp = 16.dp,
    val radiusLarge: Dp = 24.dp,
    val controlHeight: Dp = 48.dp,
    val listItemMinHeight: Dp = 56.dp,
)

data class SakuyaThemeTokens(
    val colors: SakuyaColorTokens,
    val dimensions: SakuyaDimensionTokens = SakuyaDimensionTokens(),
)

internal val LightTokens = SakuyaThemeTokens(
    colors = SakuyaColorTokens(
        elevatedSurface = Color(0xFFFFFFFF),
        pressedSurface = Color(0xFFF0ECE6),
        success = Color(0xFF356A4A),
        onSuccess = Color.White,
        warning = Color(0xFF8B5A12),
        onWarning = Color.White,
    ),
)

internal val DarkTokens = SakuyaThemeTokens(
    colors = SakuyaColorTokens(
        elevatedSurface = Color(0xFF252422),
        pressedSurface = Color(0xFF35322F),
        success = Color(0xFFA0D8AE),
        onSuccess = Color(0xFF12371E),
        warning = Color(0xFFFFCC80),
        onWarning = Color(0xFF482A00),
    ),
)

private val LocalSakuyaTokens = staticCompositionLocalOf { LightTokens }

/**
 * 职责说明：为无法访问 CompositionLocal 的场景提供尺寸令牌。
 *
 * 使用场景：Modifier 扩展函数、drawBehind/Canvas 等绘制 lambda、顶层常量 ——
 * 这些位置无法调用 SakuyaTheme.tokens（它是 @Composable 属性）。
 *
 * 注意：本对象**只暴露尺寸**。颜色必须走 SakuyaTheme.tokens.colors ——
 * 颜色需随深浅主题变化，静态单例会导致主题切换后颜色不更新；
 * 尺寸不随主题变化，因此静态实例是安全的。
 */
val SakuyaDimensions: SakuyaDimensionTokens = SakuyaDimensionTokens()

object SakuyaTheme {
    val tokens: SakuyaThemeTokens
        @Composable get() = LocalSakuyaTokens.current
}

internal val ProvidesSakuyaTokens = LocalSakuyaTokens
