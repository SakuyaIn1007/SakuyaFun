package com.sakuya.feed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.model.profile.RelationshipUserDto
import com.sakuya.feed.viewmodel.PublicRelationshipAction
import com.sakuya.feed.viewmodel.PublicRelationshipUiState
import com.sakuya.feed.viewmodel.PublicRelationshipViewModel
import com.sakuya.ui.component.AppSecondaryTopBar

/**
 * PublicRelationshipScreen.kt
 * 职责说明：展示指定他人主页的关注或粉丝名单，并承接列表内的用户跳转和关注操作。
 * 执行流程：路由传入 userId 和列表类型 -> ViewModel 分页请求 -> 用户行可进入主页，按钮可更新关注状态。
 * 错误处理：首屏失败显示重试页；加载更多失败保留已显示内容，并在列表底部提供重试入口。
 */
@Composable
fun PublicRelationshipScreen(
    userId: String,
    followers: Boolean,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: PublicRelationshipViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(userId, followers) { viewModel.onAction(PublicRelationshipAction.Load(userId, followers)) }

    PublicRelationshipContent(
        title = if (followers) "粉丝" else "关注",
        state = state,
        onBack = onBack,
        onUserClick = onUserClick,
        onToggleFollow = { viewModel.onAction(PublicRelationshipAction.ToggleFollow(it)) },
        onLoadMore = { viewModel.onAction(PublicRelationshipAction.LoadMore) },
        onRetry = { viewModel.onAction(PublicRelationshipAction.Retry) },
    )
}

@Composable
private fun PublicRelationshipContent(
    title: String,
    state: PublicRelationshipUiState,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onToggleFollow: (RelationshipUserDto) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppSecondaryTopBar(title = title, onBack = onBack)
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.users.isEmpty() -> RelationshipEmptyState(state.errorMessage, onRetry)
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.users, key = RelationshipUserDto::userId) { user ->
                    PublicRelationshipUserRow(
                        user = user,
                        isOperating = state.operatingUserId == user.userId,
                        onUserClick = { onUserClick(user.userId) },
                        onToggleFollow = { onToggleFollow(user) },
                    )
                }
                item {
                    LaunchedEffect(state.users.size, state.canLoadMore) {
                        if (state.canLoadMore) onLoadMore()
                    }
                    when {
                        state.isLoadingMore -> Box(
                            Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                        state.errorMessage != null -> Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                            Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("重试") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipEmptyState(errorMessage: String?, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            errorMessage ?: "暂无用户",
            color = if (errorMessage == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
        )
        if (errorMessage != null) {
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("重试") }
        }
    }
}

/** 用户行将整行点击与关注按钮分离，按钮事件不会改变当前页面的导航目标。 */
@Composable
private fun PublicRelationshipUserRow(
    user: RelationshipUserDto,
    isOperating: Boolean,
    onUserClick: () -> Unit,
    onToggleFollow: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val avatarColor = if (user.avatarColor == 0L) MaterialTheme.colorScheme.primary else Color(user.avatarColor)
        Box(Modifier.size(42.dp).background(avatarColor, CircleShape), contentAlignment = Alignment.Center) {
            Text(user.initial, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.titleSmall)
            Text(user.description.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Button(
            onClick = onToggleFollow,
            enabled = !isOperating,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (user.isFollowing) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(if (isOperating) "处理中" else if (user.isFollowing) "已关注" else "关注")
        }
    }
}
