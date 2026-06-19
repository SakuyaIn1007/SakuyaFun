package com.sakuya.home.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakuya.home.model.ContentItem
import com.sakuya.home.ui.components.BannerCarousel
import com.sakuya.ui.component.ContentCard
import com.sakuya.home.ui.components.HomeTopBar
import com.sakuya.home.viewmodel.HomeAction
import com.sakuya.home.viewmodel.HomeEffect
import com.sakuya.home.viewmodel.HomeUiState
import com.sakuya.home.viewmodel.HomeViewModel
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToRanking: () -> Unit = {}
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is HomeEffect.NavigateToRanking) {
                onNavigateToRanking()
            }
        }
    }

    HomeContent(
        uiState = uiState,
        onSearchQueryChanged = { viewModel.onAction(HomeAction.OnSearchQueryChanged(it)) },
        onSearchClear = { viewModel.onAction(HomeAction.OnSearchClear) },
        onTabSelected = { viewModel.selectTab(it) },
        onNavigateToRanking = { viewModel.onAction(HomeAction.OnRankingClick) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeContent(
    uiState: HomeUiState = HomeUiState(),
    onSearchQueryChanged: (String) -> Unit = {},
    onSearchClear: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onNavigateToRanking: () -> Unit = {}
) {
    val tabs = listOf("推荐", "时间表", "轻小说")

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            item(key = "search_bar") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 2.dp,
                    color = Color.Transparent,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                ) {
                    HomeTopBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        onFeatureClick = onNavigateToRanking
                    )
                }
            }

            stickyHeader(key = "tab_bar") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 32.dp, end = 32.dp, bottom = 8.dp, top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = uiState.selectedTab == index
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
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
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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
            }

            item(key = "tab_content") {
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
                    AnimatedContent(
                        targetState = uiState.selectedTab,
                        transitionSpec = {
                            (fadeIn() + slideInVertically { it / 8 }) togetherWith
                            (fadeOut() + slideOutVertically { -it / 8 })
                        }
                    ) { tab ->
                        when (tab) {
                            0 -> HomeRecommendContent(uiState.recommendItems)
                            1 -> NovelPublishContent()
                            2 -> NovelContent(uiState.novelItems)
                            else -> HomeRecommendContent(uiState.recommendItems)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeRecommendContent(
    items: List<ContentItem> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BannerCarousel()

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "为你推荐",
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

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    SakuyaInAndroidTheme(true) {
        HomeContent()
    }
}
