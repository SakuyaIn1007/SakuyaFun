package com.sakuya.conversation.data.repository

import com.sakuya.conversation.data.remote.ConversationApiService
import com.sakuya.conversation.data.remote.ConversationDto
import com.sakuya.model.chat.Conversation
import com.sakuya.data.local.dao.ConversationDao
import com.sakuya.data.local.entity.ConversationEntity
import com.sakuya.model.network.BaseResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val apiService: ConversationApiService,
    private val conversationDao: ConversationDao
) {
    /**
     * 会话列表优先由 Room 提供。
     * 执行流程：ViewModel 订阅缓存立即渲染，网络刷新成功后覆盖写入，界面自动得到最新内容。
     */
    fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { entities -> entities.map(ConversationEntity::toDomain) }

    suspend fun refreshConversations(): Result<Unit> {
        return try {
            val conversations = apiService.getConversations()
                .toResult()
                .getOrThrow()
                .map { it.toDomainModel() }

            conversationDao.upsertAll(conversations.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    title = title,
    lastMessage = lastMessage,
    timeLabel = timeLabel,
    unreadCount = unreadCount,
    avatarText = avatarText ?: "?",
    isPinned = isPinned,
)

private fun ConversationDto.toDomainModel(): Conversation {
    return Conversation(
        id = id,
        title = title,
        lastMessage = lastMessage,
        timeLabel = timeLabel,
        unreadCount = unreadCount,
        avatarText = avatarText,
        isPinned = isPinned
    )
}

private fun Conversation.toEntity(): ConversationEntity {
    return ConversationEntity(
        id = id,
        title = title,
        lastMessage = lastMessage,
        timeLabel = timeLabel,
        unreadCount = unreadCount,
        avatarText = avatarText,
        isPinned = isPinned,
        lastActiveTime = System.currentTimeMillis()
    )
}

private fun <T> Response<BaseResponse<T>>.toResult(): Result<T> {
    val body = body()
    return if (isSuccessful && body != null) {
        body.toResult()
    } else {
        Result.failure(Exception("HTTP ${code()}"))
    }
}
