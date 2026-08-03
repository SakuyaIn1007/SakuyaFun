package com.sakuya.reader.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sakuya.navigation.READER_ARG_BOOK_ID
import com.sakuya.navigation.READER_ARG_PATH
import com.sakuya.navigation.READER_FULL_ROUTE
import com.sakuya.reader.ui.ReaderScreen

fun NavGraphBuilder.readerNavGraph(navController: NavHostController) {
    composable(
        route = READER_FULL_ROUTE,
        arguments = listOf(
            navArgument(READER_ARG_BOOK_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(READER_ARG_PATH) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val rawPath = backStackEntry.arguments?.getString(READER_ARG_PATH)
        val bookId = backStackEntry.arguments?.getString(READER_ARG_BOOK_ID)
        val filePath = rawPath
            ?.takeIf { it.isNotEmpty() }
            .orEmpty()

        ReaderScreen(
            bookId = bookId,
            filePath = filePath,
            onBack = { navController.popBackStack() }
        )
    }
}
