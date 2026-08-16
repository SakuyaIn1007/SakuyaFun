package com.sakuya.friend.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.friend.viewmodel.FriendViewModel

/**
 * AddFriendScreen.kt
 * 职责说明：承载添加好友页面的导航入口，并将页面事件交由 FriendViewModel 处理。
 * 执行流程：导航层进入本 Screen -> 注入 ViewModel -> AddFriendContent 订阅状态并渲染搜索与添加结果。
 */
@Composable
fun AddFriendScreen(
    viewModel: FriendViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) = AddFriendContent(viewModel = viewModel, onBack = onBack, modifier = modifier)
