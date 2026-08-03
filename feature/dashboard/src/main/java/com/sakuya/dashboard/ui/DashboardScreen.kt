package com.sakuya.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.ui.component.PrimaryTabRow
import com.sakuya.ui.theme.SakuyaInAndroidTheme

/** The bottom-navigation landing page; discovery remains in the Light Novel feature. */
@Composable
fun DashboardScreen(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(1) }
    DashboardContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onSearchClick = onSearchClick,
        modifier = modifier
    )
}

@Composable
fun DashboardContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            PrimaryTabRow(
                tabs = listOf("关注", "推荐"),
                selectedIndex = selectedTab,
                onTabSelected = onTabSelected,
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
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun DashboardFeedContent(selectedTab: Int, modifier: Modifier = Modifier) {
    val isFollowing = selectedTab == 0
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = if (isFollowing) "关注动态" else "推荐动态",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isFollowing) {
                "这里会展示你关注的人发布的动态。"
            } else {
                "这里会展示为你推荐的阅读动态与社区活动。"
            },
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    SakuyaInAndroidTheme(true) {
        DashboardContent(
            selectedTab = 1,
            onTabSelected = {},
            onSearchClick = {}
        )
    }
}