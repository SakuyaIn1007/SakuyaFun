package com.sakuya.sakuyainandroid.navigation

import androidx.annotation.DrawableRes
import com.sakuya.conversation.navigation.CONVERSATION_ROUTE
import com.sakuya.designsystem.icon.SakuyaIcons
import com.sakuya.friend.navigation.FRIEND_ROUTE
import com.sakuya.profile.navigation.PROFILE_ROUTE

data class ToplevelNavItem(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int
)

val topLevelNavItems = listOf(
    ToplevelNavItem(
        route = CONVERSATION_ROUTE,
        label = "消息",
        iconRes = SakuyaIcons.Conversation
    ),
    ToplevelNavItem(
        route = FRIEND_ROUTE,
        label = "好友",
        iconRes = SakuyaIcons.Friends
    ),
    ToplevelNavItem(
        route = PROFILE_ROUTE,
        label = "我的",
        iconRes = SakuyaIcons.Settings
    )
)
