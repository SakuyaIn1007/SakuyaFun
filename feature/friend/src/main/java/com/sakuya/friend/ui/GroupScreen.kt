package com.sakuya.friend.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.friend.viewmodel.GroupViewModel
import com.sakuya.model.group.GroupConversation

/**
 * GroupScreen.kt
 * 职责说明：提供群聊列表页面的独立导航入口。
 * 执行流程：导航层进入群聊页 -> GroupViewModel 加载群会话 -> 点击群聊后将领域模型回传给导航层。
 */
@Composable
fun GroupScreen(
    onBack: () -> Unit = {},
    onGroupClick: (GroupConversation) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GroupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    GroupContent(
        groups = uiState.conversations,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onGroupClick = onGroupClick,
        modifier = modifier,
    )
}
