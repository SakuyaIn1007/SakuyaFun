package com.sakuya.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.home.ui.HomeScreen
import com.sakuya.home.ui.RankingScreen
import com.sakuya.navigation.HOME_ROUTE
import com.sakuya.navigation.RANKING_ROUTE

fun NavGraphBuilder.homeNavGraph(navController: NavHostController) {
    composable(HOME_ROUTE) {
        HomeScreen(
            onNavigateToRanking = {
                navController.navigate(RANKING_ROUTE)
            }
        )
    }
    composable(RANKING_ROUTE) {
        RankingScreen(
            onBack = { navController.popBackStack() }
        )
    }
}
