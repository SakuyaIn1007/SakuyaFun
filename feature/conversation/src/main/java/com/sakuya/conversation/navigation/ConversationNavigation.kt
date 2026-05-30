package com.sakuya.conversation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sakuya.conversation.model.Conversation
import com.sakuya.conversation.ui.ChatScreen
import com.sakuya.conversation.ui.ConversationScreen

fun NavGraphBuilder.conversationNavGraph(navController: NavHostController) {
    val conversations = sampleConversations()

    composable(CONVERSATION_ROUTE) {
        ConversationScreen(
            conversations = conversations,
            showBackButton = false,
            onBack = { navController.popBackStack() },
            onConversationClick = { conversation ->
                navController.navigate(chatRoute(conversation.id))
            }
        )
    }
    composable(CHAT_ROUTE_PATTERN) { backStackEntry ->
        val conversationId = backStackEntry.arguments?.getString(CHAT_ID_ARG)
        val title = conversations.firstOrNull { it.id == conversationId }?.title ?: "聊天"

        ChatScreen(
            title = title,
            onBack = { navController.popBackStack() }
        )
    }
}

private fun sampleConversations() = listOf(
    Conversation(
        id = "family",
        title = "家人群",
        lastMessage = "晚饭已经准备好了，记得早点回来。",
        timeLabel = "18:42",
        unreadCount = 3,
        avatarText = "家",
        isPinned = true
    ),
    Conversation(
        id = "sakuya",
        title = "十六夜咲夜",
        lastMessage = "明天的清单我整理好了。",
        timeLabel = "17:08",
        avatarText = "咲"
    ),
    Conversation(
        id = "work",
        title = "项目讨论",
        lastMessage = "feature 的初版可以先走本地 mock 数据。",
        timeLabel = "昨天",
        unreadCount = 1,
        avatarText = "项"
    ),
    Conversation(
        id = "system",
        title = "系统通知",
        lastMessage = "账号安全保护已开启。",
        timeLabel = "周三",
        avatarText = "通"
    )
)
