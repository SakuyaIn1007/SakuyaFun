package com.sakuya.conversation.data.repository

import com.sakuya.conversation.data.remote.ConversationApiService
import com.sakuya.conversation.data.remote.ConversationDto
import com.sakuya.conversation.model.Conversation
import com.sakuya.data.local.dao.ConversationDao
import com.sakuya.data.local.entity.ConversationEntity
import com.sakuya.model.network.BaseResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val apiService: ConversationApiService,
    private val conversationDao: ConversationDao
) {
    suspend fun getConversations(): Result<List<Conversation>> {
        return try {
            val conversations = apiService.getConversations()
                .toResult()
                .getOrThrow()
                .map { it.toDomainModel() }
                .sortedWith(compareByDescending<Conversation> { it.isPinned })

            conversationDao.upsertAll(conversations.map { it.toEntity() })
            Result.success(conversations)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = conversationDao.observeAll()
                .first()
                .map { entity ->
                    Conversation(
                        id = entity.id,
                        title = entity.title,
                        lastMessage = entity.lastMessage,
                        timeLabel = entity.timeLabel,
                        unreadCount = entity.unreadCount,
                        avatarText = entity.avatarText ?: "?",
                        isPinned = entity.isPinned
                    )
                }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }
}

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
