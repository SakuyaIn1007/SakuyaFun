package com.sakuya.ui.component

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.designsystem.icon.SakuyaIcons
import com.sakuya.model.feed.DynamicPost
import com.sakuya.ui.theme.SakuyaInAndroidTheme

private const val MaxVisibleImages = 3

/** Shared presentation for a post in the home feed and a user's own timeline. */
@Composable
fun DynamicPostCard(
    post: DynamicPost,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onImageClick: (imageIndex: Int) -> Unit = {},
    onNotInterested: () -> Unit = {},
    onReport: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onAuthorClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        DynamicAuthor(post, onNotInterested, onReport, onEdit, onDelete, onAuthorClick)
        Text(
            text = post.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            text = post.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        DynamicImageGrid(
            imageColors = post.imageColors,
            onImageClick = onImageClick,
            modifier = Modifier.padding(top = 12.dp),
        )
        DynamicPostFooter(
            tags = post.tags,
            commentCount = post.commentCount,
            likeCount = post.likeCount,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun DynamicAuthor(
    post: DynamicPost,
    onNotInterested: () -> Unit,
    onReport: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAuthorClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(post.authorColor)).clickable(onClick = onAuthorClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(post.authorInitial, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(8.dp))
        Text(post.authorName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.clickable(onClick = onAuthorClick))
        Spacer(Modifier.weight(1f))
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    painter = painterResource(SakuyaIcons.MoreHoriz),
                    contentDescription = "更多操作",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                if (post.isMine) {
                    DropdownMenuItem(text = { Text("编辑") }, onClick = { menuExpanded = false; onEdit() })
                    DropdownMenuItem(text = { Text("删除") }, onClick = { menuExpanded = false; onDelete() })
                } else {
                DropdownMenuItem(
                    text = { Text("不感兴趣") },
                    onClick = {
                        menuExpanded = false
                        onNotInterested()
                    },
                )
                DropdownMenuItem(
                    text = { Text("举报") },
                    onClick = {
                        menuExpanded = false
                        onReport()
                    },
                )
                }
            }
        }
    }
}

/** Shows at most three images and only displays the total when images are hidden. */
@Composable
fun DynamicImageGrid(
    imageColors: List<Long>,
    modifier: Modifier = Modifier,
    onImageClick: (imageIndex: Int) -> Unit = {},
) {
    if (imageColors.isEmpty()) return

    val visibleImages = imageColors.take(MaxVisibleImages)
    val hasHiddenImages = imageColors.size > visibleImages.size
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        visibleImages.forEachIndexed { index, color ->
            DynamicImagePlaceholder(
                color = Color(color),
                totalImageCount = if (hasHiddenImages && index == visibleImages.lastIndex) imageColors.size else null,
                onClick = { onImageClick(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DynamicImagePlaceholder(
    color: Color,
    totalImageCount: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("阅读日常", color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.labelMedium)
        if (totalImageCount != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(bottomStart = 8.dp),
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Text(
                    text = "共 $totalImageCount 张",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DynamicPostFooter(
    tags: List<String>,
    commentCount: Int,
    likeCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        DynamicTagRow(tags = tags, modifier = Modifier.weight(1f))
        DynamicStat(SakuyaIcons.Comment, commentCount, "评论数")
        Spacer(Modifier.width(14.dp))
        DynamicStat(SakuyaIcons.Thumb, likeCount, "点赞数")
    }
}

@Composable
fun DynamicTagRow(tags: List<String>, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        tags.forEach { tag ->
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DynamicStat(icon: Int, count: Int, contentDescription: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(5.dp))
        Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun DynamicPostCardPreview() {
    SakuyaInAndroidTheme(darkTheme = true) {
        DynamicPostCard(
            post = DynamicPost(
                id = "preview-post",
                authorName = "月见草",
                authorInitial = "月",
                authorColor = 0xFF8B7BBE,
                title = "雨天读完《山茶文具店》",
                content = "像收到一封温柔的信。书里那些替人写下的话，让这个潮湿的下午也慢慢安静下来。",
                imageColors = listOf(0xFFC9AD8D, 0xFF7D9B94, 0xFFB98773, 0xFF9B729F),
                tags = listOf("#读书笔记", "#治愈系"),
                commentCount = 18,
                likeCount = 76,
            ),
        )
    }
}
