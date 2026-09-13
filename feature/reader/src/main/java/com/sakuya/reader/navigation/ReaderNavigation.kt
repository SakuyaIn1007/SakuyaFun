package com.sakuya.reader.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sakuya.navigation.READER_ARG_BOOK_ID
import com.sakuya.navigation.READER_ARG_PATH
import com.sakuya.navigation.READER_FULL_ROUTE
import com.sakuya.navigation.WENKU8_FULL_READER_ROUTE
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
        // 目录保留在返回栈中；ViewModel 会将旧 wenku8:* 进度迁移到稳定 content:* 键。
        ReaderScreen(bookId = "content:$novelId", filePath = "", remoteChapter = Triple(novelId, chapterId, title), onBack = { navController.popBackStack() })
    }
    // 整本阅读：chapterId 传 null，阅读器从头连续阅读，不做目标章定位。
    composable(WENKU8_FULL_READER_ROUTE, arguments = listOf(navArgument(READER_ARG_BOOK_ID) { type = NavType.StringType })) { entry ->
        val novelId = entry.arguments?.getString(READER_ARG_BOOK_ID).orEmpty()
        ReaderScreen(bookId = "content:$novelId", filePath = "", remoteChapter = Triple(novelId, null, ""), onBack = { navController.popBackStack() })
    }
}
