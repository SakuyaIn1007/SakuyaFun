package com.sakuya.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sakuya.catalog.model.ContentItem
import com.sakuya.ui.component.ContentCard

/**
 * 年度专题与实时排行榜分开呈现：这里的名次在同一届内固定，不展示升降趋势。
 */
@Composable
fun LightNovelAwardContent(
    items: List<ContentItem>,
    onBookClick: (bookId: String) -> Unit = {}
) {
    val topTwenty = items.distinctBy(ContentItem::id).take(20)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    text = "轻小说大赏",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "年度典藏 Top 20 · 每届发布后固定归档",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "与日榜、周榜等实时热度榜单分开浏览",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(topTwenty.size) { index ->
            val item = topTwenty[index]
            ContentCard(
                title = "${index + 1}. ${item.title}",
                subtitle = item.subtitle,
                rating = item.rating,
                tags = item.tags,
                index = index,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { onBookClick(item.id) }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
