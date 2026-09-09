package com.sakuya.profile.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.profile.model.RelationshipListType
import com.sakuya.profile.model.RelationshipUser
import com.sakuya.profile.viewmodel.RelationshipAction
import com.sakuya.profile.viewmodel.RelationshipEffect
import com.sakuya.profile.viewmodel.RelationshipUiState
import com.sakuya.profile.viewmodel.RelationshipViewModel
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.component.UpdateDot

/**
 * ProfileRelationshipScreen.kt
 * 职责说明：渲染关注和粉丝分页列表，不保留 UI 私有用户模型或样例数据。
 * 执行流程：路由指定列表类型 -> RelationshipViewModel 加载分页数据 -> 按钮派发关注状态更新 Action。
 */
@Composable
fun ProfileRelationshipScreen(
    isFollowing: Boolean,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RelationshipViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val type = if (isFollowing) RelationshipListType.FOLLOWING else RelationshipListType.FOLLOWERS
    LaunchedEffect(type) { viewModel.onAction(RelationshipAction.Load(type)) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            if (effect is RelationshipEffect.ShowError) errorMessage = effect.message
        }
    }
    RelationshipContent(
        title = if (isFollowing) "关注" else "粉丝",
        uiState = uiState,
        errorMessage = errorMessage,
        onToggleFollow = { viewModel.onAction(RelationshipAction.ToggleFollow(it)) },
        onUserClick = onUserClick,
        onLoadMore = { viewModel.onAction(RelationshipAction.LoadMore) },
        onRetry = {
            errorMessage = null
            viewModel.onAction(RelationshipAction.Load(type))
        },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun RelationshipContent(
    title: String,
    uiState: RelationshipUiState,
    errorMessage: String?,
    onToggleFollow: (RelationshipUser) -> Unit,
    onUserClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppSecondaryTopBar(title = title, onBack = onBack)
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.users.isEmpty() -> RelationshipEmptyState(errorMessage, onRetry)
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(uiState.users, key = RelationshipUser::userId) { user ->
                    RelationshipUserRow(
                        user = user,
                        isOperating = uiState.operatingUserId == user.userId,
                        onToggleFollow = { onToggleFollow(user) },
                        onUserClick = { onUserClick(user.userId) },
                    )
                }
                item {
                    LaunchedEffect(uiState.users.size) { if (uiState.canLoadMore) onLoadMore() }
                    if (uiState.isLoadingMore) Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

@Composable
private fun RelationshipEmptyState(errorMessage: String?, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(errorMessage ?: "暂无用户", color = MaterialTheme.colorScheme.outline)
        if (errorMessage != null) Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("重试") }
    }
}

/** 单行只根据领域 isFollowing 渲染，点击按钮后由 ViewModel 按服务端结果回写列表。 */
@Composable
private fun RelationshipUserRow(
    user: RelationshipUser,
    isOperating: Boolean,
    onToggleFollow: () -> Unit,
    onUserClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(Color(user.avatarColor), CircleShape), contentAlignment = Alignment.Center) {
            Text(user.initial, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment=Alignment.CenterVertically){Text(user.name, style = MaterialTheme.typography.titleSmall);if(user.hasUnseenPosts){Spacer(Modifier.width(6.dp));UpdateDot()}}
            Text(user.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Button(
            onClick = onToggleFollow,
            enabled = !isOperating,
            colors = ButtonDefaults.buttonColors(containerColor = if (user.isFollowing) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary),
        ) {
            Text(if (isOperating) "处理中" else if (user.isFollowing) "已关注" else "关注")
        }
    }
}
