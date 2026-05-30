package com.sakuya.friend.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.friend.model.Friend
import com.sakuya.friend.ui.FriendScreen

fun NavGraphBuilder.friendNavGraph(navController: NavHostController) {
    composable(FRIEND_ROUTE) {
        FriendScreen(
            friends = sampleFriends(),
            showBackButton = false,
            onBack = { navController.popBackStack() }
        )
    }
}

private fun sampleFriends() = listOf(
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
    Friend(
        id = "patchouli",
        name = "帕秋莉",
        status = "阅读中",
        avatarText = "帕",
        isOnline = true
    ),
    Friend(
        id = "meiling",
        name = "红美铃",
        status = "昨天",
        avatarText = "美"
    )
)
