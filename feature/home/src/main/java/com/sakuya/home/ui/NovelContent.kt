package com.sakuya.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.home.model.ContentItem
import com.sakuya.home.ui.components.ContentCard
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun NovelScreen(
    items: List<ContentItem> = emptyList()
) {
    NovelContent(items)
}

@Composable
fun NovelContent(
    items: List<ContentItem> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "分类题材、文库、排序",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        items.forEachIndexed { index, item ->
            ContentCard(
                title = item.title,
                subtitle = item.subtitle,
                rating = item.rating,
                tags = item.tags,
                index = index,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
fun NovelPreview() {
    SakuyaInAndroidTheme(true) {
        NovelContent()
    }
}
