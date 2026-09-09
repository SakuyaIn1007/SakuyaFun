package com.sakuya.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.BadgedBox
import com.sakuya.ui.theme.SakuyaTheme

/**
 * Shared first-level tab treatment for Dashboard, Light Novel and Profile.
 * 选中态只改变字号；背景、文字颜色和字重保持一致，避免切换时出现额外色块。
 */
@Composable
fun PrimaryTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badgeIndices: Set<Int> = emptySet(),
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    Row(modifier = modifier) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            val dimensions = SakuyaTheme.tokens.dimensions
            BadgedBox(
                badge={if(index in badgeIndices)UpdateDot()},
                modifier=Modifier.clickable{onTabSelected(index)}.padding(horizontal=dimensions.spaceMd,vertical=dimensions.spaceSm),
            ){
                Text(
                    text=title,
                    style=MaterialTheme.typography.titleMedium.copy(fontSize=if(selected)19.sp else 16.sp),
                    color=MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        trailingContent()
    }
}
