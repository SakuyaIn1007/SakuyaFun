package com.sakuya.catalog.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.catalog.ui.CatalogScreen
import com.sakuya.catalog.ui.CatalogScheduleScreen
import com.sakuya.catalog.ui.LightNovelAwardScreen
import com.sakuya.navigation.BOOK_DETAIL_ARG_ID
import com.sakuya.navigation.BOOK_DETAIL_BASE_ROUTE
import com.sakuya.navigation.CATALOG_ROUTE
import com.sakuya.navigation.CATALOG_SCHEDULE_ROUTE
import com.sakuya.navigation.LIGHT_NOVEL_AWARD_ROUTE
import com.sakuya.navigation.SEARCH_ROUTE

fun NavGraphBuilder.catalogNavGraph(navController: NavHostController) {
    composable(CATALOG_ROUTE) {
        CatalogScreen(
            onNavigateToSearch = { navController.navigate(SEARCH_ROUTE) },
            onNavigateToSchedule = { navController.navigate(CATALOG_SCHEDULE_ROUTE) },
            onNavigateToAward = { navController.navigate(LIGHT_NOVEL_AWARD_ROUTE) },
            onNavigateToBookDetail = { bookId ->
                navController.navigate(
                    "$BOOK_DETAIL_BASE_ROUTE?$BOOK_DETAIL_ARG_ID=${Uri.encode(bookId)}"
                )
            }
        )
    }
    composable(CATALOG_SCHEDULE_ROUTE) {
        CatalogScheduleScreen(
            onBack = { navController.popBackStack() },
            onBookClick = { bookId ->
                navController.navigate(
                    "$BOOK_DETAIL_BASE_ROUTE?$BOOK_DETAIL_ARG_ID=${Uri.encode(bookId)}"
                )
            },
        )
    }
    composable(LIGHT_NOVEL_AWARD_ROUTE) {
        LightNovelAwardScreen(
            onBack = { navController.popBackStack() },
            onBookClick = { bookId ->
                navController.navigate(
                    "$BOOK_DETAIL_BASE_ROUTE?$BOOK_DETAIL_ARG_ID=${Uri.encode(bookId)}"
                )
            }
        )
    }
}
