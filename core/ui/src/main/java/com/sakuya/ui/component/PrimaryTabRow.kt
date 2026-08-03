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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared first-level tab treatment for Dashboard, Light Novel and Profile.
 * The visual state is intentionally expressed by typography only.
 */
@Composable
fun PrimaryTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    Row(modifier = modifier) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            Text(
                text = title,
                modifier = Modifier
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (selected) 19.sp else 16.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Spacer(Modifier.weight(1f))
        trailingContent()
    }
}
