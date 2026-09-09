package com.sakuya.search.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.sakuya.navigation.BOOK_DETAIL_ARG_ID
import com.sakuya.navigation.BOOK_DETAIL_BASE_ROUTE
import com.sakuya.navigation.SEARCH_ROUTE
import com.sakuya.navigation.SEARCH_RESULTS_ARG_QUERY
import com.sakuya.navigation.SEARCH_RESULTS_BASE_ROUTE
import com.sakuya.navigation.SEARCH_RESULTS_ROUTE
import com.sakuya.navigation.feedDetailRoute
import com.sakuya.navigation.feedAuthorRoute
import com.sakuya.navigation.WENKU8_DETAIL_BASE_ROUTE
import com.sakuya.navigation.WENKU8_DETAIL_ARG_ID
import com.sakuya.navigation.WENKU8_DETAIL_ROUTE
import com.sakuya.navigation.WENKU8_READER_BASE_ROUTE
import com.sakuya.navigation.READER_ARG_BOOK_ID
import com.sakuya.navigation.READER_ARG_CHAPTER_ID
import com.sakuya.navigation.READER_ARG_TITLE
import com.sakuya.search.ui.SearchScreen
import com.sakuya.search.ui.SearchResultsScreen
import com.sakuya.search.ui.Wenku8DetailScreen

/**
 * 职责说明：注册搜索入口与结果页，并将页面点击事件转换为应用导航。
 * 执行流程：输入关键词进入结果页 -> 轻小说 ID 作为查询参数进入书籍详情 -> 动态 ID 经 FeedRoutes 编码后进入动态详情。
 */
fun NavGraphBuilder.searchNavGraph(navController: NavHostController) {
    composable(SEARCH_ROUTE) {
        SearchScreen(
            onBack = { navController.popBackStack() },
            onSearch = { query -> navController.navigate("$SEARCH_RESULTS_BASE_ROUTE?$SEARCH_RESULTS_ARG_QUERY=${Uri.encode(query)}") },
        )
    }
    composable(
        route = SEARCH_RESULTS_ROUTE,
        arguments = listOf(navArgument(SEARCH_RESULTS_ARG_QUERY) { type = NavType.StringType }),
    ) { entry ->
        SearchResultsScreen(
            query = entry.arguments?.getString(SEARCH_RESULTS_ARG_QUERY).orEmpty(),
            onBack = { navController.popBackStack() },
            onBookClick = { bookId -> navController.navigate("$WENKU8_DETAIL_BASE_ROUTE?$WENKU8_DETAIL_ARG_ID=${Uri.encode(bookId)}") },
            onDynamicClick = { postId -> navController.navigate(feedDetailRoute(postId)) },
            onUserClick = { userId -> navController.navigate(feedAuthorRoute(userId)) },
        )
    }
    composable(route = WENKU8_DETAIL_ROUTE, arguments = listOf(navArgument(WENKU8_DETAIL_ARG_ID) { type = NavType.StringType })) {
        val novelId = it.arguments?.getString(WENKU8_DETAIL_ARG_ID).orEmpty()
        Wenku8DetailScreen(onBack = { navController.popBackStack() }, onChapterClick = { chapterId, title ->
            navController.navigate("$WENKU8_READER_BASE_ROUTE?$READER_ARG_BOOK_ID=${Uri.encode(novelId)}&$READER_ARG_CHAPTER_ID=${Uri.encode(chapterId)}&$READER_ARG_TITLE=${Uri.encode(title)}")
        })
    }
}
