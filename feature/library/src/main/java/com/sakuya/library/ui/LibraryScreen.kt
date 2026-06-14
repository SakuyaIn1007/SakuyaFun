package com.sakuya.library.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.home.ui.components.ContentCard
import com.sakuya.library.model.LibraryItem
import com.sakuya.library.model.LibraryItemType
import com.sakuya.library.viewmodel.LibraryViewModel
import com.sakuya.ui.component.AppPrimaryTopBar
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val novelItems by viewModel.novelItems.collectAsState()
    val musicItems by viewModel.musicItems.collectAsState()

    LibraryContent(
        selectedTab = selectedTab,
        novelItems = novelItems,
        musicItems = musicItems,
        onTabSelected = { viewModel.selectTab(it) },
        onRemoveItem = { viewModel.removeItem(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    selectedTab: Int,
    novelItems: List<LibraryItem>,
    musicItems: List<LibraryItem>,
    onTabSelected: (Int) -> Unit,
    onRemoveItem: (String) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            AppPrimaryTopBar(
                title = "我的收藏"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            LibraryTabBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { it / 4 }) togetherWith
                    (fadeOut() + slideOutHorizontally { -it / 4 })
                }
            ) { tab ->
                when (tab) {
                    0 -> LibraryContentList(
                        items = novelItems,
                        onRemoveItem = onRemoveItem
                    )
                    1 -> LibraryContentList(
                        items = musicItems,
                        onRemoveItem = onRemoveItem
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("轻小说", "乐馆")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 72.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                shadowElevation = if (isSelected) 6.dp else 0.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected)
                            FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryContentList(
    items: List<LibraryItem>,
    onRemoveItem: (String) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无收藏内容",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ContentCard(
                        title = item.title,
                        subtitle = item.subtitle,
                        rating = item.rating,
                        tags = item.tags,
                        index = items.indexOf(item)
                    )
                }
                IconButton(
                    onClick = { onRemoveItem(item.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "取消收藏",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryContentPreview() {
    SakuyaInAndroidTheme(true) {
        LibraryContent(
            selectedTab = 0,
            novelItems = listOf(
                LibraryItem(
                    id = "novel_1",
                    title = "刀剑神域",
                    subtitle = "川原砾 · 电击文库",
                    rating = 4.8f,
                    tags = listOf("科幻", "冒险"),
                    type = LibraryItemType.NOVEL,
                    collectedAt = "2026-06-01"
                )
            ),
            musicItems = emptyList(),
            onTabSelected = {},
            onRemoveItem = {}
        )
    }
}
