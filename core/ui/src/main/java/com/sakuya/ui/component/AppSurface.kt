package com.sakuya.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sakuya.ui.theme.SakuyaTheme

/**
 * AppSurface.kt
 * 职责说明：
 * 1. 统一内容卡片和列表项的留白、圆角及 Surface 层级，避免各页面自行拼装视觉容器。
 * 2. AppListItem 只负责点击反馈与布局，不承载业务状态和数据转换。
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dimensions = SakuyaTheme.tokens.dimensions
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(SakuyaTheme.tokens.colors.elevatedSurface)
            .padding(dimensions.spaceLg),
    ) { content() }
}

@Composable
fun AppListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val dimensions = SakuyaTheme.tokens.dimensions
    val itemModifier = modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = dimensions.listItemMinHeight)
        .clip(MaterialTheme.shapes.small)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = dimensions.spaceMd, vertical = dimensions.spaceSm)

    Row(modifier = itemModifier, verticalAlignment = Alignment.CenterVertically) {
        leading?.let {
            Box(Modifier.padding(end = dimensions.spaceMd), contentAlignment = Alignment.Center) { it() }
        }
        Column(modifier = Modifier.weight(1f)) { content() }
        trailing?.let {
            Box(Modifier.padding(start = dimensions.spaceMd), contentAlignment = Alignment.Center) { it() }
        }
    }
}
