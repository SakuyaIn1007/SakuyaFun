package com.sakuya.feed.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.feed.ui.ComposeFeedScreen
import com.sakuya.feed.ui.FeedTimeline
import com.sakuya.navigation.FEED_COMPOSE_ROUTE
import com.sakuya.navigation.FEED_ROUTE

fun NavGraphBuilder.feedNavGraph(navController: NavHostController) {
    composable(FEED_ROUTE) { FeedTimeline() }
    composable(FEED_COMPOSE_ROUTE) { ComposeFeedScreen { navController.popBackStack() } }
}
