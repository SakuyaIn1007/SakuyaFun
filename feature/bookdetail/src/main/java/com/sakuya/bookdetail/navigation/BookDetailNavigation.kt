package com.sakuya.bookdetail.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sakuya.bookdetail.ui.BookDetailScreen
import com.sakuya.navigation.BOOK_DETAIL_ARG_ID
import com.sakuya.navigation.BOOK_DETAIL_FULL_ROUTE

fun NavGraphBuilder.bookDetailNavGraph(
    navController: NavHostController,
    onStartReading: (bookId: String, filePath: String) -> Unit
) {
    composable(
        route = BOOK_DETAIL_FULL_ROUTE,
        arguments = listOf(
            navArgument(BOOK_DETAIL_ARG_ID) {
                type = NavType.StringType
            }
        )
    ) { entry ->
        val bookId = entry.arguments?.getString(BOOK_DETAIL_ARG_ID).orEmpty()
        BookDetailScreen(
            bookId = bookId,
            onBack = { navController.popBackStack() },
            onStartReading = onStartReading
        )
    }
}
