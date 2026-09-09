package com.sakuya.sakuyainandroid

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.sakuya.navigation.FEED_COMPOSE_ROUTE
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sakuya.sakuyainandroid.navigation.AppNavHost
import com.sakuya.sakuyainandroid.navigation.topLevelNavItems
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import com.sakuya.ui.motion.MotionSpec
import com.sakuya.ui.motion.motionPressScale
import com.sakuya.ui.motion.rememberMotionPreferences
import com.sakuya.ui.theme.SakuyaTheme
import com.sakuya.navigation.AUTH_LOGIN_ROUTE
import com.sakuya.navigation.CONVERSATION_ROUTE
import com.sakuya.navigation.PROFILE_ROUTE
import com.sakuya.model.notification.UpdateBadgeState
import com.sakuya.ui.component.UpdateDot
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun MainScreen(
    startDestination: String,
    pendingNotificationRoute: String? = null,
    onNotificationRouteConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel:MainScreenViewModel=hiltViewModel(),
) {
    val updateState by viewModel.state.collectAsState()
    val lifecycleOwner=LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner){
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_START)viewModel.refresh()}
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose{lifecycleOwner.lifecycle.removeObserver(observer)}
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val shouldShowBottomBar = topLevelNavItems.any { item ->
        currentDestination.isTopLevelDestination(item.route)
    }

    // 冷启动通知在登录状态确认后导航；未登录时保留目标，登录页离开后再消费。
    LaunchedEffect(pendingNotificationRoute, currentDestination?.route) {
        if (pendingNotificationRoute != null && currentDestination?.route != AUTH_LOGIN_ROUTE) {
            navController.navigate(pendingNotificationRoute) { launchSingleTop = true }
            onNotificationRouteConsumed()
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (shouldShowBottomBar) {
                MainBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateFeed = { navController.navigate(FEED_COMPOSE_ROUTE) },
                    updateState = updateState,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun MainBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit,
    onCreateFeed: () -> Unit,
    updateState:UpdateBadgeState,
) {
    val motionPreferences = rememberMotionPreferences()
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        topLevelNavItems.forEachIndexed { index, item ->
            if (index == 2) {
                val createInteractionSource = remember { MutableInteractionSource() }
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .padding(horizontal = SakuyaTheme.tokens.dimensions.spaceMd)
                        .width(58.dp)
                        .size(height = 34.dp, width = 58.dp)
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                        .motionPressScale(createInteractionSource)
                        .clickable(
                            interactionSource = createInteractionSource,
                            indication = null,
                            onClick = onCreateFeed,
                        ),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "发表动态", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            val selected = currentDestination.isTopLevelDestination(item.route)
            val iconTint by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(if (motionPreferences.animationsEnabled) MotionSpec.QUICK_DURATION_MILLIS else 0),
                label = "bottom-bar-icon-color",
            )
            val labelColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(if (motionPreferences.animationsEnabled) MotionSpec.QUICK_DURATION_MILLIS else 0),
                label = "bottom-bar-label-color",
            )
            NavigationBarItem(
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    // 选中态仅通过图标和文字颜色表达，不在图标底部叠加色块。
                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                onClick = { onNavigate(item.route) },
                icon = {
                    BadgedBox(badge={if((item.route==CONVERSATION_ROUTE&&updateState.showMessage)||(item.route==PROFILE_ROUTE&&updateState.showProfile))UpdateDot()}){
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(item.iconRes),
                            contentDescription = item.label,
                            tint = iconTint
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        color = labelColor
                    )
                }
            )
        }
    }
}

private fun NavDestination?.isTopLevelDestination(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}



@Preview(showBackground = true)
@Composable
fun MainBottomBarPreview() {
    SakuyaInAndroidTheme(darkTheme = true) {
        val mockDestination = NavDestination("").apply {
            route = topLevelNavItems.firstOrNull()?.route
        }

        MainBottomBar(
            currentDestination = mockDestination,
            onNavigate = { /* Preview 里面空实现就行喵 */ },
            onCreateFeed = {},
            updateState = UpdateBadgeState(),
        )
    }
}
