package com.sakuya.sakuyainandroid.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.sakuya.navigation.CATALOG_ROUTE
import com.sakuya.navigation.CONVERSATION_ROUTE
import com.sakuya.navigation.DASHBOARD_ROUTE
import com.sakuya.navigation.PROFILE_ROUTE
import com.sakuya.ui.motion.MotionSpec
import com.sakuya.ui.motion.rememberMotionPreferences

/**
 * AppNavHost.kt
 * 职责说明：
 * 1. 承载应用级导航图，并统一页面转场语义。
 * 2. 顶级页切换使用淡入淡出，层级页面使用方向明确的短距离位移。
 * 执行流程：导航目标变化 -> 根据目标路由判断层级 -> Navigation Compose 执行相应过渡；
 * 页面状态和路由本身不因动效而改变。
 */

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    val motionPreferences = rememberMotionPreferences()
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            if (!motionPreferences.animationsEnabled) {
                EnterTransition.None
            } else if (targetState.destination.route.isTopLevelRoute()) {
                fadeIn(tween(MotionSpec.QUICK_DURATION_MILLIS))
            } else {
                fadeIn(tween(MotionSpec.QUICK_DURATION_MILLIS)) +
                    slideInHorizontally(tween(MotionSpec.STANDARD_DURATION_MILLIS)) { it / 6 }
            }
        },
        exitTransition = {
            if (!motionPreferences.animationsEnabled) {
                ExitTransition.None
            } else if (targetState.destination.route.isTopLevelRoute()) {
                fadeOut(tween(MotionSpec.QUICK_DURATION_MILLIS))
            } else {
                fadeOut(tween(MotionSpec.QUICK_DURATION_MILLIS)) +
                    slideOutHorizontally(tween(MotionSpec.STANDARD_DURATION_MILLIS)) { -it / 6 }
            }
        },
        popEnterTransition = {
            if (!motionPreferences.animationsEnabled) {
                EnterTransition.None
            } else fadeIn(tween(MotionSpec.QUICK_DURATION_MILLIS)) +
                slideInHorizontally(tween(MotionSpec.STANDARD_DURATION_MILLIS)) { -it / 6 }
        },
        popExitTransition = {
            if (!motionPreferences.animationsEnabled) {
                ExitTransition.None
            } else fadeOut(tween(MotionSpec.QUICK_DURATION_MILLIS)) +
                slideOutHorizontally(tween(MotionSpec.STANDARD_DURATION_MILLIS)) { it / 6 }
        },
    ) {
        mainNavGraph(navController = navController)
    }
}

/** 顶级页无方向层级关系，避免底栏切换出现明显翻页感。 */
private fun String?.isTopLevelRoute(): Boolean = this in setOf(
    DASHBOARD_ROUTE,
    CONVERSATION_ROUTE,
    CATALOG_ROUTE,
    PROFILE_ROUTE,
)
