package com.sakuya.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.library.ui.LibraryScreen
import com.sakuya.navigation.LIBRARY_ROUTE

fun NavGraphBuilder.libraryNavGraph(navController: NavHostController) {
    composable(LIBRARY_ROUTE) {
        LibraryScreen()
    }
}
