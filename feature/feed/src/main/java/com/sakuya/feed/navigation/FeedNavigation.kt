package com.sakuya.feed.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import android.net.Uri
import com.sakuya.feed.ui.ComposeFeedScreen
import com.sakuya.feed.ui.FeedDetailScreen
import com.sakuya.feed.ui.PublicAuthorScreen
import com.sakuya.feed.ui.PublicRelationshipScreen
import com.sakuya.navigation.FEED_COMPOSE_ROUTE
import com.sakuya.navigation.FEED_DETAIL_POST_ID_ARG
import com.sakuya.navigation.FEED_DETAIL_ROUTE
import com.sakuya.navigation.FEED_ROUTE
import com.sakuya.navigation.BOOK_DETAIL_ARG_ID
import com.sakuya.navigation.BOOK_DETAIL_BASE_ROUTE
import com.sakuya.navigation.FEED_AUTHOR_ROUTE
import com.sakuya.navigation.FEED_USER_ID_ARG
import com.sakuya.navigation.chatRoute
import com.sakuya.navigation.feedAuthorRoute
import com.sakuya.navigation.feedDetailRoute

fun NavGraphBuilder.feedNavGraph(navController: NavHostController) {
    composable(FEED_AUTHOR_ROUTE, listOf(navArgument(FEED_USER_ID_ARG) { type = NavType.StringType })) { entry ->
        val userId = entry.arguments?.getString(FEED_USER_ID_ARG).orEmpty()
        PublicAuthorScreen(
            userId,
            onOpenFollowing = { id -> navController.navigate("feed_author_relationship/${Uri.encode(id)}/following") },
            onOpenFollowers = { id -> navController.navigate("feed_author_relationship/${Uri.encode(id)}/followers") },
            onOpenConversation = { conversationId, title ->
                navController.navigate(chatRoute(conversationId, title))
            },
            onPostClick = { postId -> navController.navigate(feedDetailRoute(postId)) },
        ) { navController.popBackStack() }
    }
    composable("feed_author_relationship/{userId}/{type}", listOf(navArgument("userId") { type = NavType.StringType }, navArgument("type") { type = NavType.StringType })) { entry ->
        PublicRelationshipScreen(
            userId = entry.arguments?.getString("userId").orEmpty(),
            followers = entry.arguments?.getString("type") == "followers",
            onBack = { navController.popBackStack() },
            onUserClick = { userId -> navController.navigate(feedAuthorRoute(userId)) },
        )
    }
    composable(FEED_ROUTE) { FeedDetailScreen(onBack = { navController.popBackStack() }) }
    composable(
        route = FEED_DETAIL_ROUTE,
        arguments = listOf(navArgument(FEED_DETAIL_POST_ID_ARG) { type = NavType.StringType }),
    ) { entry ->
        FeedDetailScreen(
            postId = entry.arguments?.getString(FEED_DETAIL_POST_ID_ARG),
            onBack = { navController.popBackStack() },
            onBookClick = { bookId ->
                navController.navigate("$BOOK_DETAIL_BASE_ROUTE?$BOOK_DETAIL_ARG_ID=${Uri.encode(bookId)}")
            },
            onAuthorClick = { userId -> navController.navigate(feedAuthorRoute(userId)) },
        )
    }
    composable(FEED_COMPOSE_ROUTE) {
        ComposeFeedScreen(
            onPublished = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
}
