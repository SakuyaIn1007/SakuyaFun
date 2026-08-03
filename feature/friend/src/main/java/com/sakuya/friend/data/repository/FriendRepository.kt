package com.sakuya.friend.data.repository

import com.sakuya.friend.data.remote.FriendApiService
import com.sakuya.friend.data.remote.FriendDto
import com.sakuya.friend.data.remote.FriendRequestDto
import com.sakuya.friend.data.remote.FriendRequestItem
import com.sakuya.friend.data.remote.DirectConversationDto
import com.sakuya.friend.model.Friend
import com.sakuya.model.network.BaseResponse
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val apiService: FriendApiService
) {
    suspend fun getFriends(): Result<List<Friend>> {
        return try {
            apiService.getFriends().toResult()
                .map { list -> list.map { it.toDomainModel() } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(keyword: String): Result<List<Friend>> {
        return try {
            apiService.searchUsers(keyword).toResult()
                .map { list -> list.map { it.toDomainModel() } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(userId: String, message: String): Result<Unit> {
        return try {
            apiService.sendFriendRequest(FriendRequestDto(userId, message)).toUnitResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFriendRequests(): Result<List<FriendRequestItem>> {
        return try {
            apiService.getFriendRequests().toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Friend> {
        return try {
            apiService.acceptFriendRequest(requestId).toResult()
                .map { it.toDomainModel() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            apiService.rejectFriendRequest(requestId).toUnitResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendId: String): Result<Unit> {
        return try {
            apiService.removeFriend(friendId).toUnitResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrCreateConversation(friendId: String): Result<DirectConversationDto> {
        return try {
            apiService.getOrCreateConversation(friendId).toResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun FriendDto.toDomainModel(): Friend {
    return Friend(
        id = id,
        name = name,
        status = status,
        avatarText = avatarText,
        isOnline = isOnline
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

private fun Response<BaseResponse<Unit>>.toUnitResult(): Result<Unit> {
    val body = body()
    return if (isSuccessful && body != null && body.isSuccess()) {
        Result.success(Unit)
    } else {
        Result.failure(Exception(body?.message ?: "HTTP ${code()}"))
    }
}
