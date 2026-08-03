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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.friend.data.remote.FriendRequestItem
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.friend.model.Friend
import com.sakuya.friend.viewmodel.FriendAction
import com.sakuya.friend.viewmodel.FriendViewModel
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.component.Outline
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun FriendScreen(
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    onBack: () -> Unit = {},
    viewModel: FriendViewModel = hiltViewModel()
) {
    val friends by viewModel.friends.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    FriendContent(
        friends = friends,
        isLoading = isLoading,
        showBackButton = showBackButton,
        onBack = onBack,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@Composable
fun AddFriendScreen(
    viewModel: FriendViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val results by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var keyword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("你好，我想添加你为好友") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppSecondaryTopBar(title = "添加好友", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = {
                    keyword = it
                    viewModel.searchUsers(it)
                },
                label = { Text("搜索昵称或用户 ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.size(12.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("验证消息") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        when {
            isLoading && results.isEmpty() -> LoadingBox()
            keyword.isBlank() -> EmptyMessage("输入关键词搜索用户")
            results.isEmpty() -> EmptyMessage("没有找到相关用户")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.id }) { friend ->
                    FriendSearchRow(
                        friend = friend,
                        onAdd = { viewModel.addFriend(friend.id, message) }
                    )
                    Outline(dp = 76.dp)
                }
            }
        }
    }
}

@Composable
fun FriendRequestsScreen(
    requests: List<FriendRequestItem>,
    isLoading: Boolean,
    onBack: () -> Unit = {},
    onAccept: (String) -> Unit = {},
    onReject: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppSecondaryTopBar(title = "好友通知", onBack = onBack)
        when {
            isLoading && requests.isEmpty() -> LoadingBox()
            requests.isEmpty() -> EmptyMessage("暂无好友请求")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(requests, key = { it.id }) { request ->
                    FriendRequestRow(
                        request = request,
                        onAccept = { onAccept(request.id) },
                        onReject = { onReject(request.id) }
                    )
                    Outline(dp = 96.dp)
                }
            }
        }
    }
}

@Composable
fun GroupScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppSecondaryTopBar(title = "群聊", onBack = onBack)
        EmptyMessage("群聊功能正在准备中")
    }
}

@Composable
fun FriendContent(
    friends: List<Friend>,
    isLoading: Boolean,
    showBackButton: Boolean = true,
    onBack: () -> Unit = {},
    onAction: (FriendAction) -> Unit = {},
    modifier: Modifier = Modifier
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
                IconButton(onClick = { onAction(FriendAction.OnAddFriendClick) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加好友"
                    )
                }
            }
        )
        if (isLoading && friends.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = "好友 ${friends.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(friends, key = { it.id }) { friend ->
                    FriendRow(
                        friend = friend,
                        onClick = { onAction(FriendAction.OnFriendClick(friend)) }
                    )
                    Outline(dp = 76.dp)
                }
            }
        }
    }
}

@Composable
private fun FriendRow(
    friend: Friend,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseOnSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FriendAvatar(text = friend.avatarText, online = friend.isOnline)
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
private fun FriendSearchRow(
    friend: Friend,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseOnSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FriendAvatar(text = friend.avatarText, online = friend.isOnline)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.name,
                style = MaterialTheme.typography.titleMedium,
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
        Button(onClick = onAdd) {
            Text("添加")
        }
    }
}

@Composable
private fun FriendRequestRow(
    request: FriendRequestItem,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseOnSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FriendAvatar(text = request.avatarText, online = false)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = request.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = request.message.ifBlank { "请求添加你为好友" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = request.createdAt,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        OutlinedButton(onClick = onReject) {
            Text("拒绝")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onAccept) {
            Text("接受")
        }
    }
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyMessage(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun FriendAvatar(
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
private fun FriendContentPreview() {
    SakuyaInAndroidTheme {
        FriendContent(
            friends = listOf(
                Friend(
                    id = "sakuya",
                    name = "十六夜咲夜",
                    status = "刚刚在线",
                    avatarText = "咲",
                    isOnline = true
                ),
                Friend(
                    id = "remilia",
                    name = "蕾米莉亚",
                    status = "今天 16:20",
                    avatarText = "蕾"
                ),
            ),
            isLoading = false,
            showBackButton = true,
            onBack = {},
            onAction = {}
        )
    }
}
