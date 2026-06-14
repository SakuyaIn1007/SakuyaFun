package com.sakuya.sakuyainandroid.navigation

import androidx.annotation.DrawableRes
import com.sakuya.navigation.CONVERSATION_ROUTE
import com.sakuya.navigation.HOME_ROUTE
import com.sakuya.navigation.LIBRARY_ROUTE
import com.sakuya.navigation.PROFILE_ROUTE

data class ToplevelNavItem(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int
)

val topLevelNavItems = listOf(
    ToplevelNavItem(
        route = HOME_ROUTE,
        label = "首页",
        iconRes = SakuyaIcons.Home
    ),
    ToplevelNavItem(
        route = LIBRARY_ROUTE,
        label = "收藏",
        iconRes = SakuyaIcons.Favorites
    ),
    ToplevelNavItem(
        route = CONVERSATION_ROUTE,
        label = "消息",
        iconRes = SakuyaIcons.Conversation
    ),
    ToplevelNavItem(
        route = PROFILE_ROUTE,
        label = "我的",
        iconRes = SakuyaIcons.Settings
    )
)
