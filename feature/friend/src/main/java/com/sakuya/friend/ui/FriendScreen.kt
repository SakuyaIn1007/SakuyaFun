package com.sakuya.friend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.friend.model.Friend
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.component.Outline
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun FriendScreen(
    friends: List<Friend>,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    onBack: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppSecondaryTopBar(
            title = "好友",
            onBack = onBack,
            showNavigationIcon = showBackButton,
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加好友"
                    )
                }
            }
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
//            item {
//                FriendActionRow(title = "新的朋友", subtitle = "查看好友申请")
//                Outline(dp = 76.dp)
//            }
//            item {
//                FriendActionRow(title = "群聊", subtitle = "进入我的群聊")
//                Outline(dp = 76.dp)
//            }
            item {
                Text(
                    text = "好友 ${friends.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            items(friends, key = { it.id }) { friend ->
                FriendRow(friend = friend)
                Outline(dp = 76.dp)
            }
        }
    }
}

//@Composable
//private fun FriendActionRow(
//    title: String,
//    subtitle: String,
//    modifier: Modifier = Modifier
//) {
//    Row(
//        modifier = modifier
//            .fillMaxWidth()
//            .background(MaterialTheme.colorScheme.inverseOnSurface)
//            .clickable {}
//            .padding(horizontal = 16.dp, vertical = 12.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        AvatarBox(text = title.first().toString(), online = false)
//        Spacer(modifier = Modifier.width(12.dp))
//        Column(modifier = Modifier.weight(1f)) {
//            Text(
//                text = title,
//                style = MaterialTheme.typography.titleMedium,
//                color = MaterialTheme.colorScheme.onBackground
//            )
//            Text(
//                text = subtitle,
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.outline,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//        }
//    }
//}

@Composable
private fun FriendRow(
    friend: Friend,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseOnSurface)
            .clickable {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarBox(text = friend.avatarText, online = friend.isOnline)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = friend.status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AvatarBox(
    text: String,
    online: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(48.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSecondary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF20C997))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendScreenPreview() {
    SakuyaInAndroidTheme {
        FriendScreen(
            friends = listOf(
                Friend(
                    id = "preview",
                    name = "十六夜咲夜",
                    status = "刚刚在线",
                    avatarText = "咲",
                    isOnline = true
                )
            )
        )
    }
}
