package com.sakuya.friend.data.remote

import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FriendApiService {

    @GET("friends")
    suspend fun getFriends(): Response<BaseResponse<List<FriendDto>>>

    @GET("friends/search")
    suspend fun searchUsers(
        @Query("keyword") keyword: String
    ): Response<BaseResponse<List<FriendDto>>>

    @POST("friends/requests")
    suspend fun sendFriendRequest(
        @Body request: FriendRequestDto
    ): Response<BaseResponse<Unit>>

    @GET("friends/requests")
    suspend fun getFriendRequests(): Response<BaseResponse<List<FriendRequestItem>>>

    @PUT("friends/requests/{id}/accept")
    suspend fun acceptFriendRequest(
        @Path("id") requestId: String
    ): Response<BaseResponse<FriendDto>>

    @PUT("friends/requests/{id}/reject")
    suspend fun rejectFriendRequest(
        @Path("id") requestId: String
    ): Response<BaseResponse<Unit>>

    @DELETE("friends/{id}")
    suspend fun removeFriend(
        @Path("id") friendId: String
    ): Response<BaseResponse<Unit>>

    @POST("friends/{id}/conversation")
    suspend fun getOrCreateConversation(
        @Path("id") friendId: String
    ): Response<BaseResponse<DirectConversationDto>>
}

data class FriendDto(
    val id: String,
    val name: String,
    val status: String,
    val avatarText: String,
    val isOnline: Boolean = false,
    val avatarUrl: String? = null
)

data class FriendRequestDto(
    val userId: String,
    val message: String = ""
)

data class FriendRequestItem(
    val id: String,
    val userId: String,
    val name: String,
    val avatarText: String,
    val message: String,
    val createdAt: String
)

data class DirectConversationDto(
    val id: String,
    val title: String
)
