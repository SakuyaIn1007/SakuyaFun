package com.sakuya.friend.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sakuya.friend.data.remote.FriendRequestItem

/**
 * FriendRequestsScreen.kt
 * 职责说明：提供好友请求列表的独立页面入口。
 * 执行流程：导航层传入请求状态与接受/拒绝事件 -> Content 渲染列表 -> 事件回传给 ViewModel。
 */
@Composable
fun FriendRequestsScreen(
    requests: List<FriendRequestItem>,
    isLoading: Boolean,
    onBack: () -> Unit = {},
    onAccept: (String) -> Unit = {},
    onReject: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) = FriendRequestsContent(requests, isLoading, onBack, onAccept, onReject, modifier)
