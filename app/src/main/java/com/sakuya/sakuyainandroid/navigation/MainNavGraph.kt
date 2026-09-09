package com.sakuya.sakuyainandroid.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.sakuyainandroid.ui.DashboardScreen
import com.sakuya.authentication.navigation.authNavGraph
import com.sakuya.bookdetail.navigation.bookDetailNavGraph
import com.sakuya.conversation.navigation.conversationNavGraph
import com.sakuya.feed.navigation.feedNavGraph
import com.sakuya.friend.navigation.friendNavGraph
import com.sakuya.catalog.navigation.catalogNavGraph
import com.sakuya.library.navigation.libraryNavGraph
import com.sakuya.navigation.AUTH_LOGIN_ROUTE
import com.sakuya.navigation.DASHBOARD_ROUTE
import com.sakuya.navigation.FEED_ROUTE
import com.sakuya.navigation.feedDetailRoute
import com.sakuya.navigation.feedAuthorRoute
import com.sakuya.navigation.SEARCH_ROUTE
import com.sakuya.navigation.PROFILE_ROUTE
import com.sakuya.navigation.READER_ARG_BOOK_ID
import com.sakuya.navigation.READER_ARG_PATH
import com.sakuya.navigation.READER_BASE_ROUTE
import com.sakuya.profile.navigation.profileNavGraph
import com.sakuya.reader.navigation.readerNavGraph
import com.sakuya.search.navigation.searchNavGraph
import com.sakuya.notification.notificationNavGraph

fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    composable(DASHBOARD_ROUTE) {
        DashboardScreen(
            onSearchClick = { navController.navigate(SEARCH_ROUTE) },
            onOpenDynamic = { postId -> navController.navigate(feedDetailRoute(postId)) },
            onOpenAuthor = { userId -> navController.navigate(feedAuthorRoute(userId)) },
        )
    }
    authNavGraph(
        navController = navController,
        onAuthenticated = {
            navController.navigate(PROFILE_ROUTE) {
                popUpTo(AUTH_LOGIN_ROUTE) {
                    inclusive = true
                }
            }
        }
    )
    catalogNavGraph(navController)
    conversationNavGraph(navController)
    searchNavGraph(navController)
    feedNavGraph(navController)
    libraryNavGraph(navController)
    profileNavGraph(navController)
    friendNavGraph(navController)
    notificationNavGraph(navController)
    readerNavGraph(navController)
    bookDetailNavGraph(
        navController = navController,
        onStartReading = { bookId, filePath ->
            navController.navigate(
                "$READER_BASE_ROUTE?$READER_ARG_BOOK_ID=${Uri.encode(bookId)}&$READER_ARG_PATH=${Uri.encode(filePath)}"
            )
        }
    )
}
