package com.sakuya.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 主题令牌回归测试：保证深浅模式保留不同层级，且核心控件尺寸不会被意外缩小。
 */
class ThemeTokensTest {
    @Test
    fun `light and dark tokens expose distinct elevated surfaces`() {
        assertNotEquals(LightTokens.colors.elevatedSurface, DarkTokens.colors.elevatedSurface)
    }

    @Test
    fun `interactive controls retain accessible minimum height`() {
        assertTrue(LightTokens.dimensions.controlHeight.value >= 48f)
        assertTrue(LightTokens.dimensions.listItemMinHeight.value >= 56f)
    }

    @Test
    fun `non composable entry point mirrors the light dimension tokens`() {
        // SakuyaDimensions 供绘制 lambda / Modifier 扩展使用，必须与 light tokens 数值一致，
        // 否则同一处间距在布局与绘制两条路径上取值不同。
        assertEquals(LightTokens.dimensions.spaceXs, SakuyaDimensions.spaceXs)
        assertEquals(LightTokens.dimensions.spaceSm, SakuyaDimensions.spaceSm)
        assertEquals(LightTokens.dimensions.spaceMd, SakuyaDimensions.spaceMd)
        assertEquals(LightTokens.dimensions.spaceLg, SakuyaDimensions.spaceLg)
        assertEquals(LightTokens.dimensions.spaceXl, SakuyaDimensions.spaceXl)
    }

    @Test
    fun `radius and spacing tokens are independent fields even when values coincide`() {
        // spaceLg 与 radiusMedium 都是 16.dp，但语义不同（间距 vs 圆角）。
        // 锁定二者独立存在，防止将来有人「合并同名值」而破坏语义区分。
        assertEquals(16.dp, LightTokens.dimensions.spaceLg)
        assertEquals(16.dp, LightTokens.dimensions.radiusMedium)
        // radiusSmall(10) 与任意 space token 都不相等，因此圆角必须有自己的 token。
        assertEquals(10.dp, LightTokens.dimensions.radiusSmall)
    }
}
