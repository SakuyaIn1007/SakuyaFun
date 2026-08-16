package com.sakuya.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sakuya.model.feed.DynamicPost
import com.sakuya.ui.component.DynamicPostCard

private val recommendedPosts = listOf(
    DynamicPost(
        id = "recommended-1",
        authorName = "月见草",
        authorInitial = "月",
        authorColor = 0xFF8B7BBE,
        title = "雨天读完《山茶文具店》",
        content = "像收到一封温柔的信。书里那些替人写下的话，让这个潮湿的下午也慢慢安静下来。",
        imageColors = listOf(0xFFC9AD8D, 0xFF7D9B94, 0xFFB98773, 0xFF9B729F, 0xFF486981),
        tags = listOf("#读书笔记", "#治愈系"),
        commentCount = 18,
        likeCount = 76,
    ),
    DynamicPost(
        id = "recommended-2",
        authorName = "林间风",
        authorInitial = "林",
        authorColor = 0xFF5D8C77,
        title = "给八月列了一张待读清单",
        content = "想把阅读的速度放慢一点，留些空白给散步和发呆。你们最近在读什么？",
        imageColors = listOf(0xFF6E879A, 0xFFD5A46D),
        tags = listOf("#待读书单", "#八月阅读"),
        commentCount = 32,
        likeCount = 104,
    ),
    DynamicPost(
        id = "recommended-3",
        authorName = "南枝",
        authorInitial = "南",
        authorColor = 0xFFC06A67,
        title = "书店角落的新发现",
        content = "在旧书架里翻到一本有前任读者批注的诗集，隔着许多年，竟像和陌生人完成了一场对话。",
        tags = listOf("#书店漫游"),
        commentCount = 9,
        likeCount = 41,
    ),
)

private val followingPosts = recommendedPosts.filter { it.id != "recommended-3" }

@Composable
fun DashboardDynamicContent(
    isFollowing: Boolean,
    modifier: Modifier = Modifier,
    onPostClick: (DynamicPost) -> Unit = {},
) {
    val posts = if (isFollowing) followingPosts else recommendedPosts
    LazyColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        items(posts, key = DynamicPost::id) { post ->
            DynamicPostCard(post = post, onClick = { onPostClick(post) })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        }
    }
}
