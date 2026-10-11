package com.sakuya.reader.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ReaderOverflowMenu.kt
 * 职责说明：以文本下拉菜单承载阅读器的次要操作，避免顶栏堆叠过多图标而挤压横向空间。
 * 执行流程：ReaderScreen 按会话与文档类型组装 ReaderMenuItem 列表 -> 本组件只渲染并回传点击，
 * 不承担任何业务条件判断，因此可独立预览与复用。
 */
data class ReaderMenuItem(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    /** 用于「同步失败」等需要用错误色强调的状态项。 */
    val isError: Boolean = false,
    /** 在语义分组之间插入分隔线（正文操作 / 同步 / 离线下载）。 */
    val showDividerAbove: Boolean = false,
)

@Composable
fun ReaderOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<ReaderMenuItem>,
    /** 与阅读主题一致的菜单底色；项文字色需另行指定，否则会跟随 app 主题造成对比度丢失。 */
    containerColor: Color,
    contentColor: Color,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = containerColor,
    ) {
        items.forEach { item ->
            if (item.showDividerAbove) HorizontalDivider()
            val itemColor = if (item.isError) MaterialTheme.colorScheme.error else contentColor
            DropdownMenuItem(
                text = { Text(text = item.text, color = itemColor) },
                // 先关闭菜单再执行动作，避免动作触发的重组与菜单关闭竞争。
                onClick = {
                    onDismiss()
                    item.onClick()
                },
                enabled = item.enabled,
                colors = MenuDefaults.itemColors(
                    textColor = itemColor,
                    disabledTextColor = contentColor.copy(alpha = DISABLED_ALPHA),
                ),
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.38f
