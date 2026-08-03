package com.sakuya.sakuyainandroid.navigation

import androidx.annotation.DrawableRes
import com.sakuya.designsystem.icon.SakuyaIcons
import com.sakuya.navigation.CONVERSATION_ROUTE
import com.sakuya.navigation.DASHBOARD_ROUTE
import com.sakuya.navigation.CATALOG_ROUTE
import com.sakuya.navigation.PROFILE_ROUTE

data class ToplevelNavItem(
    val route: String,
    val label: String,
    @DrawableRes val iconRes: Int
)

val topLevelNavItems = listOf(
    ToplevelNavItem(
        route = DASHBOARD_ROUTE,
        label = "首页",
        iconRes = SakuyaIcons.Home
    ),
    ToplevelNavItem(
        route = CONVERSATION_ROUTE,
        label = "消息",
        iconRes = SakuyaIcons.Conversation
    ),
    ToplevelNavItem(
        route = CATALOG_ROUTE,
        label = "轻小说",
        iconRes = SakuyaIcons.Folder
    ),
    ToplevelNavItem(
        route = PROFILE_ROUTE,
        label = "我的",
        iconRes = SakuyaIcons.Settings
    )
)
