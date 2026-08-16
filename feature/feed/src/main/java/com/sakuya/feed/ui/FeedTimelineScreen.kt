package com.sakuya.feed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.feed.viewmodel.FeedTimelineUiState
import com.sakuya.feed.viewmodel.FeedTimelineAction
import com.sakuya.feed.viewmodel.FeedTimelineEffect
import com.sakuya.feed.viewmodel.FeedTimelineViewModel
import com.sakuya.model.feed.DynamicPost
import com.sakuya.model.feed.FeedStream
import com.sakuya.ui.component.DynamicPostCard
import com.sakuya.ui.theme.SakuyaInAndroidTheme

/**
 * 动态首页入口。
 *
 * 页面只消费 ViewModel 状态：首次加载、错误重试与加载更多都由数据层状态驱动，避免 UI 内维护固定列表。
 */
@Composable
fun FeedTimelineScreen(
    stream: FeedStream,
    onPostClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FeedTimelineViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            if (effect is FeedTimelineEffect.ShowError) errorMessage = effect.message
        }
    }
    LaunchedEffect(stream) {
        viewModel.onAction(FeedTimelineAction.ChangeStream(stream))
    }
    FeedTimelineContent(
        uiState = uiState,
        errorMessage = errorMessage,
        onPostClick = onPostClick,
        onAuthorClick = onAuthorClick,
        onRefresh = { viewModel.onAction(FeedTimelineAction.Refresh) },
        onLoadMore = { viewModel.onAction(FeedTimelineAction.LoadMore) },
        modifier = modifier,
    )
}

@Composable
private fun FeedTimelineContent(
    uiState: FeedTimelineUiState,
    errorMessage: String?,
    onPostClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        uiState.isOfflineWithoutCache -> FeedMessageState(
            message = "当前无网络连接",
            actionLabel = "重新加载",
            onAction = onRefresh,
            modifier = modifier,
        )
        uiState.posts.isEmpty() -> FeedMessageState(
            message = errorMessage ?: "暂时还没有动态",
            actionLabel = if (errorMessage == null) null else "重新加载",
            onAction = onRefresh,
            modifier = modifier,
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        ) {
            if (uiState.isShowingCachedContent) {
                item {
                    Text(
                        text = "当前无网络，正在展示离线缓存",
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
            items(uiState.posts, key = DynamicPost::id) { post ->
                DynamicPostCard(post = post, onClick = { onPostClick(post.id) }, onAuthorClick = { onAuthorClick(post.userId) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            }
            item {
                // 列表滚动到底部后才触发下一页，避免首次进入重复请求。
                LaunchedEffect(uiState.posts.size) {
                    if (uiState.canLoadMore) onLoadMore()
                }
                if (uiState.isLoadingMore) {
                    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedMessageState(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(message, color = MaterialTheme.colorScheme.outline)
            actionLabel?.let { Button(onClick = onAction) { Text(it) } }
        }
    }
}

/** Preview 使用静态状态，只验证列表层级与卡片布局。 */
@Preview(showBackground = true)
@Composable
private fun FeedTimelineContentPreview() {
    SakuyaInAndroidTheme {
        FeedTimelineContent(
            uiState = FeedTimelineUiState(
                stream = FeedStream.RECOMMENDED,
                posts = listOf(
                    DynamicPost(
                        id = "preview",
                        authorName = "月见草",
                        authorInitial = "月",
                        authorColor = 0xFF8B7BBE,
                        title = "雨天读完《山茶文具店》",
                        content = "像收到一封温柔的信。",
                    )
                ),
                canLoadMore = false,
            ),
            errorMessage = null,
            onPostClick = {},
            onAuthorClick = {},
            onRefresh = {},
            onLoadMore = {},
        )
    }
}
