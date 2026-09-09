package com.sakuya.ui.theme

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
}
