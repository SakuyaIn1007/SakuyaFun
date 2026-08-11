package com.sakuya.catalog.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakuya.catalog.model.ContentItem
import com.sakuya.catalog.R
import com.sakuya.catalog.viewmodel.CatalogUiState
import com.sakuya.catalog.viewmodel.CatalogViewModel
import com.sakuya.ui.component.PrimaryTabRow
import com.sakuya.ui.component.ContentCard
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CatalogScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToBookDetail: (bookId: String) -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToAward: () -> Unit = {}
) {
    val viewModel: CatalogViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CatalogContent(
        uiState = uiState,
        onTabSelected = { viewModel.selectTab(it) },
        onNavigateToSearch = onNavigateToSearch,
        onBookClick = onNavigateToBookDetail,
        onNavigateToSchedule = onNavigateToSchedule,
        onNavigateToAward = onNavigateToAward
    )
}

/**
 * CatalogScreen.kt
 * 职责说明：承载轻小说内容列表，统一由目录页负责题材列表的展示与书籍点击分发。
 * 执行流程：CatalogContent 选择轻小说 Tab -> 调用本 Content -> ContentCard 点击后回传书籍 ID。
 */
@Composable
fun NovelContent(
    items: List<ContentItem> = emptyList(),
    onBookClick: (bookId: String) -> Unit = {},
) = CatalogNovelContent(items = items, onBookClick = onBookClick)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CatalogContent(
    uiState: CatalogUiState = CatalogUiState(),
    onTabSelected: (Int) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onBookClick: (bookId: String) -> Unit = {},
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToAward: () -> Unit = {}
) {
    val tabs = listOf("推荐", "榜单")

    Scaffold(
        topBar = {
            PrimaryTabRow(
                tabs = tabs,
                selectedIndex = uiState.selectedTab,
                onTabSelected = onTabSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)
            ) {
                IconButton(onClick = onNavigateToSearch) { Icon(Icons.Default.Search, "搜索") }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxWidth().padding(innerPadding)) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    uiState.error?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    AnimatedContent(
                        targetState = uiState.selectedTab,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it / 8 }) togetherWith
                            (fadeOut() + slideOutVertically { -it / 8 })
                        }
                    ) { tab ->
                        when (tab) {
                            0 -> CatalogRecommendContent(
                                items = uiState.recommendItems,
                                onBookClick = onBookClick,
                                onScheduleClick = onNavigateToSchedule,
                                onAwardClick = onNavigateToAward
                            )
                            1 -> RankingTabContent(onBookClick)
                            else -> CatalogRecommendContent(
                                items = uiState.recommendItems,
                                onBookClick = onBookClick,
                                onScheduleClick = onNavigateToSchedule,
                                onAwardClick = onNavigateToAward
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
fun CatalogRecommendContent(
    items: List<ContentItem> = emptyList(),
    onBookClick: (bookId: String) -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onAwardClick: () -> Unit = {}
) {
    val popularItems = items.take(4)
    val seasonalAdaptations = items.drop(4).take(3)
    val newReleases = items.drop(7)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { CatalogQuickLinks(onScheduleClick, onAwardClick) }
        item { CatalogSectionTitle("热门推荐", "正在被读者关注的作品") }
        catalogBookGridRows(popularItems, startIndex = 0, onBookClick = onBookClick)
        item { CatalogSectionTitle("本季度新番原作", "追番前，先从原作开始") }
        catalogBookGridRows(seasonalAdaptations, startIndex = popularItems.size, onBookClick = onBookClick)
        item { CatalogSectionTitle("新书抢鲜", "刚上架，先读为快") }
        catalogBookGridRows(
            newReleases,
            startIndex = popularItems.size + seasonalAdaptations.size,
            onBookClick = onBookClick
        )
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CatalogQuickLinks(
    onScheduleClick: () -> Unit,
    onAwardClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CatalogQuickLinkCard(
            title = "时间表",
            iconRes = R.drawable.ic_schedule,
            onClick = onScheduleClick
        )
        CatalogQuickLinkCard(
            title = "轻小说大赏",
            iconRes = R.drawable.ic_award,
            onClick = onAwardClick
        )
    }
}

@Composable
private fun CatalogQuickLinkCard(
    title: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .height(42.dp)
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(5.dp))
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.Bottom),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CatalogSectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun LazyListScope.catalogBookGridRows(
    books: List<ContentItem>,
    startIndex: Int,
    onBookClick: (bookId: String) -> Unit
) {
    items((books.size + 1) / 2) { rowIndex ->
        val firstIndex = rowIndex * 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CatalogGridBook(
                item = books[firstIndex],
                index = startIndex + firstIndex,
                modifier = Modifier.weight(1f),
                onBookClick = onBookClick
            )
            if (firstIndex + 1 < books.size) {
                CatalogGridBook(
                    item = books[firstIndex + 1],
                    index = startIndex + firstIndex + 1,
                    modifier = Modifier.weight(1f),
                    onBookClick = onBookClick
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CatalogGridBook(
    item: ContentItem,
    index: Int,
    modifier: Modifier,
    onBookClick: (bookId: String) -> Unit
) {
    val coverColors = gridCoverColors[index % gridCoverColors.size]
    Row(
        modifier = modifier
            .height(96.dp)
            .clickable { onBookClick(item.id) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 66.dp, height = 92.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(coverColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.title.take(2),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = item.tags.take(2).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

private val gridCoverColors = listOf(
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFF6B8E23), Color(0xFF4CAF50)),
    listOf(Color(0xFFE06341), Color(0xFFF4A261)),
    listOf(Color(0xFF457B9D), Color(0xFF1D3557)),
    listOf(Color(0xFF9B5DE5), Color(0xFFF15BB5)),
    listOf(Color(0xFF00B4D8), Color(0xFF0077B6))
)

private fun List<ContentItem>.filterByQuery(query: String): List<ContentItem> {
    if (query.isBlank()) return this
    return filter { item ->
        item.title.contains(query, ignoreCase = true) ||
            item.subtitle.contains(query, ignoreCase = true) ||
            item.tags.any { it.contains(query, ignoreCase = true) }
    }
}

@Preview(showBackground = true)
@Composable
fun CatalogPreview() {
    SakuyaInAndroidTheme(true) {
        CatalogContent()
    }
}
