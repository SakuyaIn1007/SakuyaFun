package com.sakuya.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sakuya.ui.component.AppSecondaryTopBar

@Composable
fun ProfileReadingHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val histories = remember {
        listOf(
            ReadingHistoryItem("月光下的旅人", "读到 第 12 章", "今天 14:20"),
            ReadingHistoryItem("Android Compose 实战", "读到 状态管理", "昨天 22:08"),
            ReadingHistoryItem("时间的碎片", "读到 第 3 卷", "周二 09:31"),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppSecondaryTopBar(
            title = "阅读记录",
            onBack = onBack
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(histories) { item ->
                ReadingHistoryRow(item = item)
            }
        }
    }
}

@Composable
private fun ReadingHistoryRow(
    item: ReadingHistoryItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseOnSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.progress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.timeText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private data class ReadingHistoryItem(
    val title: String,
    val progress: String,
    val timeText: String
)
