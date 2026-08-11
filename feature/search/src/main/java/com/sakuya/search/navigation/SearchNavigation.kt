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
import com.sakuya.search.ui.SearchScreen
import com.sakuya.search.ui.SearchResultsScreen

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
            onBookClick = { bookId -> navController.navigate("$BOOK_DETAIL_BASE_ROUTE?$BOOK_DETAIL_ARG_ID=${Uri.encode(bookId)}") },
            onDynamicClick = { postId -> navController.navigate(feedDetailRoute(postId)) },
        )
    }
}
