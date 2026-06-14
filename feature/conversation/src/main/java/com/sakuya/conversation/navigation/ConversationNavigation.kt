package com.sakuya.conversation.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sakuya.conversation.ui.ChatScreen
import com.sakuya.conversation.ui.ConversationScreen
import com.sakuya.conversation.viewmodel.ConversationEffect
import com.sakuya.conversation.viewmodel.ConversationViewModel
import com.sakuya.navigation.CHAT_ID_ARG
import com.sakuya.navigation.CHAT_ROUTE
import com.sakuya.navigation.CONVERSATION_ROUTE
import com.sakuya.navigation.FNOTICE_ROUTE
import com.sakuya.navigation.FRIEND_ROUTE
import com.sakuya.navigation.GROUP_ROUTE
import com.sakuya.navigation.chatRoute

fun NavGraphBuilder.conversationNavGraph(navController: NavHostController) {
    composable(CONVERSATION_ROUTE) {
        val viewModel: ConversationViewModel = hiltViewModel()

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
                    is ConversationEffect.ShowError -> {}
                }
            }
        }

        ConversationScreen(viewModel = viewModel)
    }
    composable(
        route = "$CHAT_ROUTE/{$CHAT_ID_ARG}?title={$CHAT_TITLE_ARG}",
        arguments = listOf(
            navArgument(CHAT_ID_ARG) { type = NavType.StringType },
            navArgument(CHAT_TITLE_ARG) {
                type = NavType.StringType
                defaultValue = ""
            }
        )
    ) {
        ChatScreen(
            onBack = { navController.popBackStack() }
        )
    }
}

private const val CHAT_TITLE_ARG = "title"
