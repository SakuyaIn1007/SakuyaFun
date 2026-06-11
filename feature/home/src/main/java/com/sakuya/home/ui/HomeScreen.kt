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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.home.ui.components.BannerCarousel
import com.sakuya.home.ui.components.ContentCard
import com.sakuya.home.ui.components.HomeTopBar
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen() {
    val tabs = listOf("推荐", "轻小说", "乐馆")
    var selectedTab by remember { mutableStateOf(0) }

    val searchBarColor = MaterialTheme.colorScheme.surface
    val tabAreaColor = MaterialTheme.colorScheme.background

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {

                        Column {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shadowElevation = 2.dp,
                                color = Color.Transparent,
                                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                            ) {
                                HomeTopBar(
                                    containerColor = searchBarColor,
                                    onFeatureClick = { }
                                )
                            }


                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(tabAreaColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 72.dp, end = 72.dp, bottom = 8.dp),
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
                                        ){
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedTab = index }
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
                        }
                }
                
            }



            item {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 8 }) togetherWith
                        (fadeOut() + slideOutVertically { -it / 8 })
                    }
                ) { tab ->
                    when (tab) {
                        0 -> HomeContent()
                        1 -> NovelContent()
                        2 -> MusicContent()
                    }
                }
            }
        }
    }
}

@Composable
fun HomeContent() {
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

        val items = listOf(
            Triple("刀剑神域", "川原砾 · 电击文库", 4.8f to listOf("科幻", "冒险", "虚拟现实")),
            Triple("关于我转生变成史莱姆这档事", "伏濑 · GC Novels", 4.6f to listOf("异世界", "奇幻", "轻松")),
            Triple("Re:从零开始的异世界生活", "长月达平 · MF文库J", 4.7f to listOf("异世界", "悬疑", "轮回")),
            Triple("无职转生", "理不尽な孫の手 · MF Books", 4.5f to listOf("异世界", "成长", "冒险")),
            Triple("盾之勇者成名录", "アネコユサギ · MF Books", 4.3f to listOf("异世界", "复仇", "冒险")),
            Triple("为美好的世界献上祝福", "暁なつめ · 角川Sneaker", 4.4f to listOf("异世界", "搞笑", "冒险")),
            Triple("OVERLORD", "丸山くがね · Enterbrain", 4.7f to listOf("异世界", "黑暗", "奇幻")),
            Triple("青春猪头少年系列", "鸭志田一 · 电击文库", 4.6f to listOf("恋爱", "青春", "校园")),
            Triple("欢迎来到实力至上主义的教室", "衣笠彰梧 · MF文库J", 4.5f to listOf("校园", "智斗", "悬疑")),
            Triple("吹响吧！上低音号", "武田绫乃 · 宝岛社", 4.4f to listOf("音乐", "青春", "校园")),
        )

        items.forEachIndexed { index, item ->
            ContentCard(
                title = item.first,
                subtitle = item.second,
                rating = item.third.first,
                tags = item.third.second,
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
        HomeScreen()
    }
}