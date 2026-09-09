package com.sakuya.conversation.navigation

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sakuya.conversation.ui.ChatScreen
import com.sakuya.conversation.ui.ChatDetailScreen
import com.sakuya.conversation.ui.ChatHistorySearchScreen
import com.sakuya.conversation.ui.ConversationScreen
import com.sakuya.conversation.viewmodel.ConversationEffect
import com.sakuya.conversation.viewmodel.ConversationViewModel
import com.sakuya.navigation.CHAT_ID_ARG
import com.sakuya.navigation.CHAT_FOCUS_MESSAGE_ID_ARG
import com.sakuya.navigation.CHAT_DETAIL_ROUTE_PATTERN
import com.sakuya.navigation.CHAT_ROUTE_PATTERN
import com.sakuya.navigation.CHAT_SEARCH_ROUTE_PATTERN
import com.sakuya.navigation.CHAT_TITLE_ARG
import com.sakuya.navigation.CONVERSATION_ROUTE
import com.sakuya.navigation.FNOTICE_ROUTE
import com.sakuya.navigation.FRIEND_ROUTE
import com.sakuya.navigation.GROUP_ROUTE
import com.sakuya.navigation.chatRoute
import com.sakuya.navigation.chatDetailRoute
import com.sakuya.navigation.chatSearchRoute

fun NavGraphBuilder.conversationNavGraph(navController: NavHostController) {
    composable(CONVERSATION_ROUTE) {
        val viewModel: ConversationViewModel = hiltViewModel()
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ConversationEffect.NavigateToChat -> {
                        navController.navigate(
                            chatRoute(effect.conversation.id, effect.conversation.title)
                        )
                    }
                    ConversationEffect.NavigateToNotice -> navController.navigate(FNOTICE_ROUTE)
                    ConversationEffect.NavigateToFriend -> navController.navigate(FRIEND_ROUTE)
                    ConversationEffect.NavigateToGroup -> navController.navigate(GROUP_ROUTE)
                    is ConversationEffect.ShowError -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        ConversationScreen(viewModel = viewModel)
    }
    composable(
        route = CHAT_ROUTE_PATTERN,
        arguments = listOf(
            navArgument(CHAT_ID_ARG) { type = NavType.StringType },
            navArgument(CHAT_TITLE_ARG) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(CHAT_FOCUS_MESSAGE_ID_ARG) { type = NavType.StringType; defaultValue = "" }
        )
    ) {
        ChatScreen(
            onBack = {
                if (!navController.popBackStack()) {
                    navController.navigate(CONVERSATION_ROUTE) {
                        launchSingleTop = true
                    }
                }
            },
            onOpenDetail = { navController.navigate(chatDetailRoute(it.arguments?.getString(CHAT_ID_ARG).orEmpty())) }
        )
    }
    composable(route = CHAT_DETAIL_ROUTE_PATTERN, arguments = listOf(navArgument(CHAT_ID_ARG) { type = NavType.StringType })) {
        val conversationId = it.arguments?.getString(CHAT_ID_ARG).orEmpty()
        ChatDetailScreen(
            onBack = { navController.popBackStack() },
            onSearchHistory = { title -> navController.navigate(chatSearchRoute(conversationId, title)) },
            onLeftGroup = {
                navController.navigate(CONVERSATION_ROUTE) { popUpTo(CONVERSATION_ROUTE) { inclusive = false }; launchSingleTop = true }
            }
        )
    }
    composable(route = CHAT_SEARCH_ROUTE_PATTERN, arguments = listOf(navArgument(CHAT_ID_ARG) { type = NavType.StringType }, navArgument(CHAT_TITLE_ARG) { type = NavType.StringType; defaultValue = "" })) { entry ->
        val conversationId = entry.arguments?.getString(CHAT_ID_ARG).orEmpty()
        val title = entry.arguments?.getString(CHAT_TITLE_ARG).orEmpty()
        ChatHistorySearchScreen(
            onBack = { navController.popBackStack() },
            onMessageSelected = { messageId ->
                // 用完整目的地 pattern 移除原聊天页、详情页和搜索页，再以目标消息参数重建聊天页。
                // 基础字符串 chat_search 并不是已注册目的地，使用它 popUpTo 会导致错误回栈或导航异常。
                navController.navigate(chatRoute(conversationId, title, messageId)) {
                    popUpTo(CHAT_ROUTE_PATTERN) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}
