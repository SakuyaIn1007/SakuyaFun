package com.sakuya.friend.data.repository

import com.sakuya.friend.data.remote.FriendApiService
import com.sakuya.friend.data.remote.FriendDto
import com.sakuya.friend.data.remote.FriendRequestItem
import com.sakuya.friend.model.Friend
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val apiService: FriendApiService
) {
    private val mockFriends = sampleFriends().toMutableList()
    private val mockRequests = mutableListOf<FriendRequestItem>()

    suspend fun getFriends(): Result<List<Friend>> {
        delay(300)
        return Result.success(mockFriends)
    }

    suspend fun searchUsers(keyword: String): Result<List<Friend>> {
        delay(200)
        val result = mockFriends.filter {
            it.name.contains(keyword, ignoreCase = true)
        }
        return Result.success(result)
    }

    suspend fun sendFriendRequest(userId: String, message: String): Result<Unit> {
        delay(200)
        val friend = mockFriends.find { it.id == userId }
        if (friend != null) {
            mockRequests.add(
                FriendRequestItem(
                    id = "req-${System.currentTimeMillis()}",
                    userId = userId,
                    name = friend.name,
                    avatarText = friend.avatarText,
                    message = message,
                    createdAt = "刚刚"
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun getFriendRequests(): Result<List<FriendRequestItem>> {
        delay(200)
        return Result.success(mockRequests.toList())
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Friend> {
        delay(200)
        val request = mockRequests.find { it.id == requestId }
            ?: return Result.failure(Exception("请求不存在"))
        mockRequests.remove(request)
        val newFriend = Friend(
            id = request.userId,
            name = request.name,
            status = "刚刚成为好友",
            avatarText = request.avatarText,
            isOnline = false
        )
        mockFriends.add(newFriend)
        return Result.success(newFriend)
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        delay(200)
        mockRequests.removeAll { it.id == requestId }
        return Result.success(Unit)
    }

    suspend fun removeFriend(friendId: String): Result<Unit> {
        delay(200)
        mockFriends.removeAll { it.id == friendId }
        return Result.success(Unit)
    }
}

private fun sampleFriends() = listOf(
    Friend(
        id = "sakuya",
        name = "十六夜咲夜",
        status = "刚刚在线",
        avatarText = "咲",
        isOnline = true
    ),
    Friend(
        id = "remilia",
        name = "蕾米莉亚",
        status = "今天 16:20",
        avatarText = "蕾"
    ),
    Friend(
        id = "patchouli",
        name = "帕秋莉",
        status = "阅读中",
        avatarText = "帕",
        isOnline = true
    ),
    Friend(
        id = "meiling",
        name = "红美铃",
        status = "昨天",
        avatarText = "美"
    )
)
