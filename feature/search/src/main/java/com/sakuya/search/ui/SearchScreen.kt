package com.sakuya.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakuya.model.search.SearchContentType
import com.sakuya.model.search.SearchResult
import com.sakuya.search.viewmodel.SearchAction
import com.sakuya.search.viewmodel.SearchEffect
import com.sakuya.search.viewmodel.SearchUiState
import com.sakuya.search.viewmodel.SearchViewModel

/**
 * SearchScreen.kt
 * 职责说明：渲染搜索入口和搜索结果；页面不保留私有样例数据，只消费 SearchViewModel 的状态。
 * 执行流程：输入或点击历史词 -> 导航到结果页 -> ViewModel 按筛选条件加载分页结果 -> UI 渲染领域 SearchResult。
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    viewModel: SearchViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val submitSearch: () -> Unit = {
        query.trim().takeIf { it.isNotEmpty() }?.let {
            focusManager.clearFocus()
            onSearch(it)
        }
        Unit
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        SearchEntryTopBar(query, { query = it }, onBack, submitSearch)
        SearchDiscovery(
            history = uiState.history,
            hotKeywords = uiState.hotKeywords,
            onKeywordClick = onSearch,
            onClearHistory = { viewModel.onAction(SearchAction.ClearHistory) },
        )
    }
}

@Composable
internal fun SearchResultsContent(
    query: String,
    onBack: () -> Unit,
    onBookClick: (String) -> Unit = {},
    onDynamicClick: (String) -> Unit = {},
    viewModel: SearchViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(query) { viewModel.onAction(SearchAction.LoadResults(query)) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            if (effect is SearchEffect.ShowError) errorMessage = effect.message
        }
    }
    SearchResultsPageContent(
        query = query,
        uiState = uiState,
        errorMessage = errorMessage,
        onBack = onBack,
        onTypeSelected = { viewModel.onAction(SearchAction.SelectType(it)) },
        onBookClick = onBookClick,
        onDynamicClick = onDynamicClick,
        onLoadMore = { viewModel.onAction(SearchAction.LoadMore) },
        onRetry = { viewModel.onAction(SearchAction.LoadResults(query)) },
    )
}

/**
 * 职责说明：渲染可预览的搜索结果页展示层，不直接持有 ViewModel。
 * 执行流程：状态容器提供结果与事件 -> 按结果类型分发点击回调 -> 导航层跳转对应详情页。
 */
@Composable
private fun SearchResultsPageContent(
    query: String,
    uiState: SearchUiState,
    errorMessage: String?,
    onBack: () -> Unit,
    onTypeSelected: (SearchContentType?) -> Unit,
    onBookClick: (String) -> Unit,
    onDynamicClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
            Text("“$query”的搜索结果", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        SearchResultFilterRow(
            selectedType = uiState.selectedType,
            onTypeSelected = onTypeSelected,
        )
        SearchResultList(
            uiState = uiState,
            errorMessage = errorMessage,
            onBookClick = onBookClick,
            onDynamicClick = onDynamicClick,
            onLoadMore = onLoadMore,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun SearchEntryTopBar(query: String, onQueryChange: (String) -> Unit, onBack: () -> Unit, onSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
        BasicTextField(
            value = query, onValueChange = onQueryChange, singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.weight(1f).height(38.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 7.dp),
        ) { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("搜索动态或轻小说", color = MaterialTheme.colorScheme.outline, fontSize = 15.sp)
                    innerTextField()
                }
            }
        }
    }
}

@Composable
private fun SearchResultFilterRow(selectedType: SearchContentType?, onTypeSelected: (SearchContentType?) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(null to "综合", SearchContentType.DYNAMIC to "内容", SearchContentType.NOVEL to "轻小说").forEach { (type, title) ->
            Text(
                text = title, style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selectedType == type) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selectedType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onTypeSelected(type) }.padding(end = 20.dp, top = 10.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun SearchDiscovery(history: List<String>, hotKeywords: List<String>, onKeywordClick: (String) -> Unit, onClearHistory: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item { SearchKeywordSection("搜索历史", history, onKeywordClick, onClearHistory) }
        item { SearchKeywordSection("搜索发现", hotKeywords, onKeywordClick) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchKeywordSection(title: String, keywords: List<String>, onKeywordClick: (String) -> Unit, onClear: (() -> Unit)? = null) {
    Column(Modifier.padding(top = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            onClear?.takeIf { keywords.isNotEmpty() }?.let { clear -> TextButton(onClick = clear) { Text("清除") } }
        }
        if (keywords.isEmpty()) Text("暂无记录", color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 12.dp))
        else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            keywords.forEach { keyword -> FilterChip(selected = false, onClick = { onKeywordClick(keyword) }, label = { Text(keyword) }) }
        }
    }
}

@Composable
private fun SearchResultList(
    uiState: SearchUiState,
    errorMessage: String?,
    onBookClick: (String) -> Unit,
    onDynamicClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    // 结果点击按内容类型分流：轻小说由书籍详情处理，动态由 FeedRoutes 对应的详情页处理。
    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        uiState.results.isEmpty() -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(errorMessage ?: "没有找到相关内容，换个关键词试试。", color = MaterialTheme.colorScheme.outline)
            if (errorMessage != null) Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("重试") }
        }
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(uiState.results, key = SearchResult::id) { result ->
                SearchResultItem(result) {
                    when (result.type) {
                        SearchContentType.NOVEL -> onBookClick(result.id)
                        SearchContentType.DYNAMIC -> onDynamicClick(result.id)
                    }
                }
            }
            item {
                LaunchedEffect(uiState.results.size) { if (uiState.canLoadMore) onLoadMore() }
                if (uiState.isLoadingMore) Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun SearchResultItem(result: SearchResult, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.Top) {
        Text(result.type.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(44.dp).padding(top = 2.dp))
        Column(Modifier.weight(1f)) {
            Text(result.title, style = MaterialTheme.typography.titleSmall)
            Text(result.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Text(result.tags.joinToString(" · ") { "#$it" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** Preview 搜索结果页，覆盖轻小说与动态两类可点击结果。 */
@Preview(showBackground = true)
@Composable
private fun SearchResultsPagePreview() {
    SearchResultsPageContent(
        query = "夏日阅读",
        uiState = SearchUiState(
            results = listOf(
                SearchResult("novel/1", "紫罗兰永恒花园", "晓佳奈 · 为理解爱而书写信件的故事。", SearchContentType.NOVEL, listOf("奇幻", "成长")),
                SearchResult("dynamic?2", "我的夏日阅读清单", "想把阅读的速度放慢一点，留些空白给散步和发呆。", SearchContentType.DYNAMIC, listOf("待读书单")),
            ),
            canLoadMore = false,
        ),
        errorMessage = null,
        onBack = {},
        onTypeSelected = {},
        onBookClick = {},
        onDynamicClick = {},
        onLoadMore = {},
        onRetry = {},
    )
}
