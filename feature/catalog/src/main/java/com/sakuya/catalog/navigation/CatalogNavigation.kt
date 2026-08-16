package com.sakuya.catalog.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.catalog.ui.CatalogScreen
import com.sakuya.catalog.ui.CatalogScheduleScreen
import com.sakuya.catalog.ui.LightNovelAwardScreen
import com.sakuya.catalog.model.ContentItem
import com.sakuya.navigation.BOOK_DETAIL_ARG_ID
import com.sakuya.navigation.BOOK_DETAIL_BASE_ROUTE
import com.sakuya.navigation.CATALOG_ROUTE
import com.sakuya.navigation.CATALOG_SCHEDULE_ROUTE
import com.sakuya.navigation.LIGHT_NOVEL_AWARD_ROUTE
import com.sakuya.navigation.SEARCH_ROUTE
import com.sakuya.navigation.WENKU8_DETAIL_BASE_ROUTE
import com.sakuya.navigation.WENKU8_DETAIL_ARG_ID

fun NavGraphBuilder.catalogNavGraph(navController: NavHostController) {
    composable(CATALOG_ROUTE) {
        CatalogScreen(
            onNavigateToSearch = { navController.navigate(SEARCH_ROUTE) },
            onNavigateToSchedule = { navController.navigate(CATALOG_SCHEDULE_ROUTE) },
            onNavigateToAward = { navController.navigate(LIGHT_NOVEL_AWARD_ROUTE) },
            onNavigateToBookDetail = { item -> navController.navigate(catalogBookRoute(item)) }
        )
    }
    composable(CATALOG_SCHEDULE_ROUTE) {
        CatalogScheduleScreen(
            onBack = { navController.popBackStack() },
            onBookClick = { item -> navController.navigate(catalogBookRoute(item)) },
        )
    }
    composable(LIGHT_NOVEL_AWARD_ROUTE) {
        LightNovelAwardScreen(
            onBack = { navController.popBackStack() },
            onBookClick = { item -> navController.navigate(catalogBookRoute(item)) }
        )
    }
}

/**
 * Wenku8 缓存 ID 仅用于本地数据库；进入远端详情必须使用服务端返回的原始小说 ID。
 */
private fun catalogBookRoute(item: ContentItem): String {
    val sourceNovelId = item.sourceNovelId
    return if (item.source == "WENKU8" && !sourceNovelId.isNullOrBlank()) {
        "$WENKU8_DETAIL_BASE_ROUTE?$WENKU8_DETAIL_ARG_ID=${Uri.encode(sourceNovelId)}"
    } else {
        "$BOOK_DETAIL_BASE_ROUTE?$BOOK_DETAIL_ARG_ID=${Uri.encode(item.id)}"
    }
}
