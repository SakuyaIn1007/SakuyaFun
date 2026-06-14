package com.sakuya.conversation.data.repository

import com.sakuya.conversation.data.remote.ConversationApiService
import com.sakuya.conversation.data.remote.ConversationDto
import com.sakuya.conversation.model.Conversation
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val apiService: ConversationApiService
) {
    private val mockConversations = sampleConversations()

    suspend fun getConversations(): Result<List<Conversation>> {
        delay(300)
        return Result.success(mockConversations)
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
