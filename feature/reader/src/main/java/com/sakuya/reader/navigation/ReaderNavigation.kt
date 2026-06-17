package com.sakuya.reader.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.navigation.READER_ROUTE
import com.sakuya.reader.ui.ReaderScreen
import com.sakuya.reader.viewmodel.ReaderViewModel

fun NavGraphBuilder.readerNavGraph(navController: NavHostController) {
    composable(READER_ROUTE) {
        val viewModel: ReaderViewModel = hiltViewModel()
        ReaderScreen(viewModel)
    }
}
