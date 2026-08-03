package com.sakuya.library.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.library.ui.LibraryScreen
import com.sakuya.navigation.LIBRARY_ROUTE
import com.sakuya.navigation.READER_ARG_BOOK_ID
import com.sakuya.navigation.READER_ARG_PATH
import com.sakuya.navigation.READER_BASE_ROUTE

fun NavGraphBuilder.libraryNavGraph(navController: NavHostController) {
    composable(LIBRARY_ROUTE) {
        LibraryScreen(
            onOpenReader = { bookId, filePath ->
                val route =
                    "$READER_BASE_ROUTE?$READER_ARG_BOOK_ID=${Uri.encode(bookId)}&$READER_ARG_PATH=${Uri.encode(filePath)}"
                navController.navigate(route)
            }
        )
    }
}
