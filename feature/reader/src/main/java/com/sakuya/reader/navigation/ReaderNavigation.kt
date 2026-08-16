package com.sakuya.reader.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sakuya.navigation.READER_ARG_BOOK_ID
import com.sakuya.navigation.READER_ARG_PATH
import com.sakuya.navigation.READER_FULL_ROUTE
import com.sakuya.navigation.WENKU8_READER_ROUTE
import com.sakuya.navigation.READER_ARG_CHAPTER_ID
import com.sakuya.navigation.READER_ARG_TITLE
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
    composable(WENKU8_READER_ROUTE, arguments = listOf(navArgument(READER_ARG_BOOK_ID) { type = NavType.StringType }, navArgument(READER_ARG_CHAPTER_ID) { type = NavType.StringType }, navArgument(READER_ARG_TITLE) { type = NavType.StringType })) { entry ->
        val novelId = entry.arguments?.getString(READER_ARG_BOOK_ID).orEmpty()
        val chapterId = entry.arguments?.getString(READER_ARG_CHAPTER_ID).orEmpty()
        val title = entry.arguments?.getString(READER_ARG_TITLE).orEmpty()
        // 目录保留在返回栈中；阅读器只记录 wenku8:{novelId} 连续进度，chapterId 仅作为本次定位目标。
        ReaderScreen(bookId = "wenku8:$novelId", filePath = "", remoteChapter = Triple(novelId, chapterId, title), onBack = { navController.popBackStack() })
    }
}
