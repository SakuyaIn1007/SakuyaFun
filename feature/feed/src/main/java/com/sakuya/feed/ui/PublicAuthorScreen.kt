package com.sakuya.feed.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.model.profile.PublicProfileDto
import com.sakuya.feed.viewmodel.PublicAuthorAction
import com.sakuya.feed.viewmodel.PublicAuthorEffect
import com.sakuya.feed.viewmodel.PublicAuthorViewModel
import com.sakuya.model.feed.DynamicPost
import com.sakuya.ui.component.DynamicPostCard

/**
 * PublicAuthorScreen.kt
 * 职责说明：展示他人公开主页、作者动态和聚合统计，并承接关注与私信入口。
 * 执行流程：进入页面加载公开资料和动态首页 -> 列表触底加载更多 -> 点击动态交给导航层打开详情。
 * 说明：关注与粉丝数字使用“数值在上、标签在下”的布局，与我的页面保持一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicAuthorScreen(
    userId: String,
    viewModel: PublicAuthorViewModel = hiltViewModel(),
    onOpenFollowing: (String) -> Unit = {},
    onOpenFollowers: (String) -> Unit = {},
    onOpenConversation: (conversationId: String, title: String) -> Unit = { _, _ -> },
    onPostClick: (String) -> Unit = {},
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableIntStateOf(0) }
    LaunchedEffect(userId) { viewModel.onAction(PublicAuthorAction.Load(userId)) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PublicAuthorEffect.ShowError -> snackbar.showSnackbar(effect.message)
                is PublicAuthorEffect.OpenConversation -> onOpenConversation(effect.conversationId, effect.title)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { TabRow(selectedTabIndex = tab, divider = {}) { listOf("动态", "数据").forEachIndexed { index, title -> Tab(tab == index, { tab = index }, text = { Text(title) }) } } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "更多") } },
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.onAction(PublicAuthorAction.OpenConversation) },
                    modifier = Modifier.weight(1f),
                    enabled = state.profile != null && !state.isOpeningConversation,
                ) {
                    if (state.isOpeningConversation) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("正在打开")
                    } else {
                        Text("私信")
                    }
                }
                Button(onClick = { viewModel.onAction(PublicAuthorAction.ToggleFollow) }, modifier = Modifier.weight(1f), enabled = state.profile != null && !state.isOperatingFollow) {
                    Text(if (state.isOperatingFollow) "处理中" else if (state.profile?.isFollowing == true) "已关注" else "关注")
                }
            }
        },
    ) { padding ->
        val profile = state.profile
        when {
            profile == null && state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            profile == null -> AuthorLoadFailure(onRetry = { viewModel.onAction(PublicAuthorAction.Load(userId)) }, modifier = Modifier.padding(padding))
            else -> AuthorLoadedContent(
                profile = profile,
                tab = tab,
                state = state,
                onOpenFollowing = onOpenFollowing,
                onOpenFollowers = onOpenFollowers,
                onPostClick = onPostClick,
                onRetryPosts = { viewModel.onAction(PublicAuthorAction.RetryPosts) },
                onLoadMorePosts = { viewModel.onAction(PublicAuthorAction.LoadMorePosts) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

/**
 * 主页已加载内容使用单一 LazyColumn，避免作者信息与动态列表嵌套滚动导致分页触发不稳定。
 */
@Composable
private fun AuthorLoadedContent(
    profile: PublicProfileDto,
    tab: Int,
    state: com.sakuya.feed.viewmodel.PublicAuthorUiState,
    onOpenFollowing: (String) -> Unit,
    onOpenFollowers: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onRetryPosts: () -> Unit,
    onLoadMorePosts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        item { AuthorHeader(profile) }
        item {
            AuthorStats(
                profile,
                onRelationshipClick = { label ->
                    if (label == "关注") onOpenFollowing(profile.userId) else if (label == "粉丝") onOpenFollowers(profile.userId)
                },
            )
        }
        if (tab == 0) {
            item { Text("他发布的动态", Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium) }
            when {
                state.isLoadingPosts && state.posts.isEmpty() -> item { AuthorPostsLoading() }
                state.posts.isEmpty() -> item {
                    AuthorPostsMessage(
                        message = state.postsErrorMessage ?: "暂时还没有发布动态",
                        actionLabel = state.postsErrorMessage?.let { "重新加载" },
                        onAction = onRetryPosts,
                    )
                }
                else -> {
                    items(state.posts, key = DynamicPost::id) { post ->
                        DynamicPostCard(post = post, onClick = { onPostClick(post.id) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    }
                    item {
                        when {
                            state.isLoadingMorePosts -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            state.postsErrorMessage != null -> AuthorPostsMessage(state.postsErrorMessage, "重试", onRetryPosts)
                            state.canLoadMorePosts -> LaunchedEffect(state.posts.size) { onLoadMorePosts() }
                        }
                    }
                }
            }
        } else {
            item { AuthorDataContent(profile) }
        }
    }
}

@Composable
private fun AuthorHeader(profile: PublicProfileDto) {
    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF8B7BBE)), contentAlignment = Alignment.Center) {
            Text(profile.nickname.firstOrNull()?.toString() ?: "?", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.height(64.dp).weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            Text(profile.nickname, style = MaterialTheme.typography.titleMedium)
            Text(profile.signature.orEmpty().ifBlank { "这个人还没有留下签名" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * 三个统计项目与“我的”模块使用同一上下排布：大号数值在上、说明文字在下。
 * 执行流程：点击“关注”或“粉丝”会将当前主页用户 ID 传回导航层，打开对应的分页列表；获赞与收藏仅展示汇总值。
 */
@Composable
private fun AuthorStats(profile: PublicProfileDto, onRelationshipClick: (String) -> Unit) {
    val stats = listOf(
        profile.followingCount to "关注",
        profile.followerCount to "粉丝",
        profile.likesAndFavoritesCount to "获赞与收藏",
    )
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        stats.forEach { (count, label) ->
            Column(
                modifier = Modifier.weight(1f).clickable(enabled = label != "获赞与收藏") { onRelationshipClick(label) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(formatMetric(count), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(3.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AuthorDataContent(profile: PublicProfileDto) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("数据统计", style = MaterialTheme.typography.titleMedium)
        AuthorDataRow("已发布动态", profile.postCount)
        AuthorDataRow("累计获赞与收藏", profile.likesAndFavoritesCount)
        AuthorDataRow("关注数", profile.followingCount)
        AuthorDataRow("粉丝数", profile.followerCount)
    }
}

@Composable
private fun AuthorDataRow(label: String, value: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMetric(value), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun AuthorPostsLoading() {
    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun AuthorPostsMessage(message: String, actionLabel: String?, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        actionLabel?.let { Button(onClick = onAction) { Text(it) } }
    }
}

@Composable
private fun AuthorLoadFailure(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("暂时无法加载该用户主页", color = MaterialTheme.colorScheme.outline)
            Button(onClick = onRetry) { Text("重新加载") }
        }
    }
}

private fun formatMetric(value: Long): String = when {
    value >= 10_000 -> if (value % 10_000 == 0L) "${value / 10_000}w" else "%.1fw".format(value / 10_000.0)
    value >= 1_000 -> if (value % 1_000 == 0L) "${value / 1_000}k" else "%.1fk".format(value / 1_000.0)
    else -> value.toString()
}
