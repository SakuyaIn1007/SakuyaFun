package com.sakuya.sakuyainandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.dashboard.DashboardViewModel
import com.sakuya.feed.ui.FeedTimelineScreen
import com.sakuya.model.feed.FeedStream
import com.sakuya.ui.component.PrimaryTabRow
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import androidx.compose.ui.tooling.preview.Preview

/**
 * DashboardScreen.kt
 * 职责说明：底部导航落地页，聚合关注/推荐动态分流与好友更新红点。
 * 归属说明：该页面同时编排 dashboard 状态与 feed 时间线 UI，属于 app 层聚合职责，
 * 从 feature:dashboard 上移，避免 feature 模块之间产生编译依赖。
 */
@Composable
fun DashboardScreen(
    onSearchClick: () -> Unit,
    onOpenDynamic: (String) -> Unit,
    onOpenAuthor: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val updateState by viewModel.updateState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(1) }
    DashboardContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onSearchClick = onSearchClick,
        onOpenDynamic = onOpenDynamic,
        onOpenAuthor = onOpenAuthor,
        modifier = modifier,
        showFollowingUpdate = updateState.showFollowing,
    )
}

@Composable
fun DashboardContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onOpenDynamic: (String) -> Unit,
    onOpenAuthor: (String) -> Unit,
    modifier: Modifier = Modifier,
    showFollowingUpdate: Boolean = false,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            PrimaryTabRow(
                tabs = listOf("关注", "推荐"),
                selectedIndex = selectedTab,
                onTabSelected = onTabSelected,
                badgeIndices = if (showFollowingUpdate) setOf(0) else emptySet(),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)
            ) {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                }
            }
        }
    ) { innerPadding ->
        DashboardFeedContent(
            selectedTab = selectedTab,
            onOpenDynamic = onOpenDynamic,
            onOpenAuthor = onOpenAuthor,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun DashboardFeedContent(
    selectedTab: Int,
    onOpenDynamic: (String) -> Unit,
    onOpenAuthor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FeedTimelineScreen(
        stream = if (selectedTab == 0) FeedStream.FOLLOWING else FeedStream.RECOMMENDED,
        onPostClick = onOpenDynamic,
        onAuthorClick = onOpenAuthor,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    SakuyaInAndroidTheme(true) {
        DashboardContent(
            selectedTab = 1,
            onTabSelected = {},
            onSearchClick = {},
            onOpenDynamic = {},
            onOpenAuthor = {},
        )
    }
}
