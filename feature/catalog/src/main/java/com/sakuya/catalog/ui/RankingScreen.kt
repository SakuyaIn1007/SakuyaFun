package com.sakuya.catalog.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.sakuya.catalog.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakuya.catalog.model.ContentItem
import com.sakuya.catalog.model.RankingItem
import com.sakuya.catalog.viewmodel.RankingUiState
import com.sakuya.catalog.viewmodel.RankingViewModel
import com.sakuya.ui.theme.SakuyaInAndroidTheme

private enum class RankingBoard(val title: String) {
    POPULAR("人气榜"),
    NEW_RELEASES("新书榜"),
    HIGH_SCORE("高分榜"),
    COLLECTED("收藏榜")
}

private data class RankingFilters(
    val genre: String? = null,
    val period: String? = null,
    val status: String? = null,
    val rating: String? = null,
    val publisher: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingContent(
    uiState: RankingUiState,
    onRetry: () -> Unit = {},
    onBookClick: (ContentItem) -> Unit = {}
) {
    var board by remember { mutableStateOf(RankingBoard.POPULAR) }
    var filters by remember { mutableStateOf(RankingFilters()) }
    var isFilterSheetVisible by remember { mutableStateOf(false) }

    when {
        uiState.isLoading -> LoadingContent()
        uiState.error != null -> ErrorContent(uiState.error.orEmpty(), onRetry)
        else -> RankingList(
            modifier = Modifier,
            board = board,
            filters = filters,
            items = uiState.items.forRanking(board, filters),
            onBoardSelected = { board = it },
            onFilterClick = { isFilterSheetVisible = true },
            onBookClick = onBookClick
        )
    }

    if (isFilterSheetVisible) {
        RankingFilterSheet(
            filters = filters,
            onFiltersChanged = { filters = it },
            onDismiss = { isFilterSheetVisible = false }
        )
    }
}

@Composable
fun RankingTabContent(onBookClick: (ContentItem) -> Unit = {}) {
    val viewModel: RankingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RankingContent(
        uiState = uiState,
        onRetry = viewModel::retry,
        onBookClick = onBookClick
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) { CircularProgressIndicator() }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors()) { Text("重试") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RankingList(
    modifier: Modifier,
    board: RankingBoard,
    filters: RankingFilters,
    items: List<RankingItem>,
    onBoardSelected: (RankingBoard) -> Unit,
    onFilterClick: () -> Unit,
    onBookClick: (ContentItem) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { RankingBoardTabs(board, onBoardSelected) }
        item { RankingQuickFilters(filters, onFilterClick) }
        filters.summary()?.let { summary ->
            item {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }
        if (items.isEmpty()) {
            item {
                Text(
                    text = "没有符合条件的作品，试试放宽筛选条件。",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(items, key = { it.item.id }) { item ->
                Column {
                    RankingBookCard(item, onClick = { onBookClick(item.item) })
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 102.dp, end = 16.dp, top = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
                    )
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun RankingBoardTabs(selected: RankingBoard, onSelected: (RankingBoard) -> Unit) {
    val selectedIndex = RankingBoard.entries.indexOf(selected)
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 12.dp,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {}
    ) {
        RankingBoard.entries.forEach { board ->
            Tab(
                selected = selected == board,
                onClick = { onSelected(board) },
                text = {
                    Text(
                        text = board.title,
                        fontWeight = if (selected == board) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
private fun RankingQuickFilters(filters: RankingFilters, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { QuickFilterChip(filters.genre ?: "题材", onClick) }
            item { QuickFilterChip(filters.period ?: "统计周期", onClick) }
            item { QuickFilterChip(filters.status ?: "状态", onClick) }
            item { QuickFilterChip(filters.rating ?: "评分", onClick) }
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        TextButton(
            onClick = onClick,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_filter_list),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("筛选")
        }
    }
}

@Composable
private fun QuickFilterChip(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    ) {
        Text(
            text = "$label⌄",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingFilterSheet(
    filters: RankingFilters,
    onFiltersChanged: (RankingFilters) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("筛选", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "重置",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onFiltersChanged(RankingFilters()) }
                )
            }
            FilterOptionRow("题材", listOf("不限", "异世界", "校园", "恋爱", "奇幻", "科幻", "悬疑"), filters.genre) {
                onFiltersChanged(filters.copy(genre = it))
            }
            FilterOptionRow("统计周期", listOf("不限", "近 24 小时", "近 7 日", "近 30 日", "近一年"), filters.period) {
                onFiltersChanged(filters.copy(period = it))
            }
            FilterOptionRow("状态", listOf("不限", "连载中", "已完结"), filters.status) {
                onFiltersChanged(filters.copy(status = it))
            }
            FilterOptionRow("评分", listOf("不限", "4.0 分以上", "4.5 分以上", "4.8 分以上"), filters.rating) {
                onFiltersChanged(filters.copy(rating = it))
            }
            FilterOptionRow("文库", listOf("不限", "电击文库", "MF文库J", "GA文库", "GC Novels"), filters.publisher) {
                onFiltersChanged(filters.copy(publisher = it))
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("查看榜单") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterOptionRow(
    title: String,
    options: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                val isAll = option == "不限"
                FilterChip(
                    selected = if (isAll) selected == null else selected == option,
                    onClick = { onSelected(if (isAll) null else option) },
                    label = { Text(option) }
                )
            }
        }
    }
}

@Composable
private fun RankingBookCard(rankingItem: RankingItem, onClick: () -> Unit) {
    val item = rankingItem.item
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankingCover(item, rankingItem.rank)
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f).height(104.dp)) {
            Column(modifier = Modifier.padding(end = 4.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    item.description.orEmpty().ifBlank {
                        "一部围绕 ${item.tags.joinToString("、")} 展开的轻小说作品。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text("${item.author} · ${item.publisher.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(
                modifier = Modifier.align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.tags.take(2).forEach { tag ->
                    Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = "★ ${String.format("%.1f", item.rating)}",
                modifier = Modifier.align(Alignment.BottomEnd),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB300)
            )
        }
    }
}

@Composable
private fun RankingCover(item: ContentItem, rank: Int) {
    val colors = when (rank % 4) {
        0 -> listOf(Color(0xFF667EEA), Color(0xFF764BA2))
        1 -> listOf(Color(0xFFE06341), Color(0xFFF4A261))
        2 -> listOf(Color(0xFF457B9D), Color(0xFF00B4D8))
        else -> listOf(Color(0xFF6B8E23), Color(0xFF4CAF50))
    }
    Box(
        modifier = Modifier
            .size(width = 74.dp, height = 104.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        if (!item.coverRequestUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.coverRequestUrl,
                contentDescription = "${item.title} 封面",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = item.title.take(2),
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun RankingFilters.summary(): String? = listOfNotNull(genre, status, period, rating, publisher)
    .takeIf { it.isNotEmpty() }
    ?.joinToString(" · ")

private fun List<RankingItem>.forRanking(
    board: RankingBoard,
    filters: RankingFilters
): List<RankingItem> {
    val minimumRating = filters.rating?.substringBefore(" ")?.toFloatOrNull()
    val filtered = filter { ranking ->
        val item = ranking.item
        (filters.genre == null || filters.genre in item.tags) &&
            (filters.publisher == null || filters.publisher == item.publisher) &&
            (minimumRating == null || item.rating >= minimumRating)
    }
    val sorted = when (board) {
        RankingBoard.POPULAR -> filtered
        RankingBoard.NEW_RELEASES -> filtered.reversed()
        RankingBoard.HIGH_SCORE -> filtered.sortedByDescending { it.item.rating }
        RankingBoard.COLLECTED -> filtered.sortedByDescending { it.item.tags.size * it.item.rating }
    }
    return sorted.mapIndexed { index, item -> item.copy(rank = index + 1) }
}

@Preview(showBackground = true)
@Composable
fun RankingPreview() {
    SakuyaInAndroidTheme(true) {
        RankingContent(uiState = RankingUiState(isLoading = false))
    }
}
