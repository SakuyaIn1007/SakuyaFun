package com.sakuya.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.navigation.SEARCH_ROUTE
import com.sakuya.search.ui.SearchScreen

fun NavGraphBuilder.searchNavGraph(navController: NavHostController) {
    composable(SEARCH_ROUTE) { SearchScreen { navController.popBackStack() } }
}
