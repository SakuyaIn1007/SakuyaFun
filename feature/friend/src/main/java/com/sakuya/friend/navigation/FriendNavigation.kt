package com.sakuya.friend.navigation

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.friend.ui.AddFriendScreen
import com.sakuya.friend.ui.FriendScreen
import com.sakuya.friend.ui.FriendRequestsScreen
import com.sakuya.friend.ui.GroupScreen
import com.sakuya.friend.viewmodel.FriendEffect
import com.sakuya.friend.viewmodel.FriendViewModel
import com.sakuya.navigation.FRIEND_ADD_ROUTE
import com.sakuya.navigation.FRIEND_ROUTE
import com.sakuya.navigation.FNOTICE_ROUTE
import com.sakuya.navigation.GROUP_ROUTE
import com.sakuya.navigation.chatRoute

fun NavGraphBuilder.friendNavGraph(navController: NavHostController) {
    composable(FRIEND_ROUTE) {
        val viewModel: FriendViewModel = hiltViewModel()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is FriendEffect.NavigateToChat -> {
                        navController.navigate(
                            chatRoute(effect.conversationId, effect.title)
                        )
                    }
                    is FriendEffect.NavigateToAddFriend -> {
                        navController.navigate(FRIEND_ADD_ROUTE)
                    }
                    is FriendEffect.ShowError -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is FriendEffect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        FriendScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }
    composable(FRIEND_ADD_ROUTE) {
        val viewModel: FriendViewModel = hiltViewModel()
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is FriendEffect.ShowError -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is FriendEffect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> Unit
                }
            }
        }
        AddFriendScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }
    composable(FNOTICE_ROUTE) {
        val viewModel: FriendViewModel = hiltViewModel()
        val requests by viewModel.friendRequests.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.loadFriendRequests()
            viewModel.effect.collect { effect ->
                when (effect) {
                    is FriendEffect.ShowError -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is FriendEffect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> Unit
                }
            }
        }

        FriendRequestsScreen(
            requests = requests,
            isLoading = isLoading,
            onBack = { navController.popBackStack() },
            onAccept = viewModel::acceptFriendRequest,
            onReject = viewModel::rejectFriendRequest
        )
    }
    composable(GROUP_ROUTE) {
        GroupScreen(
            onBack = { navController.popBackStack() },
            // 群聊项点击后沿用聊天页，保证从群聊入口进入的返回链路与好友会话一致。
            onGroupClick = { group ->
                navController.navigate(chatRoute(group.group.id, group.group.name))
            }
        )
    }
}
