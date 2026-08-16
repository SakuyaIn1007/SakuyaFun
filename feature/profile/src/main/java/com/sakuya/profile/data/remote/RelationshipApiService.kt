package com.sakuya.profile.data.remote

import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * RelationshipApiService.kt
 * 职责说明：声明关注关系的分页读取和关注/取消关注接口。
 * 执行流程：RelationshipRepository 调用该服务并将 DTO 转为 RelationshipUser，UI 不接触网络字段。
 */
interface RelationshipApiService {
    @GET("profile/following")
    suspend fun getFollowing(@Query("page") page: Int, @Query("pageSize") pageSize: Int): Response<BaseResponse<RelationshipPageDto>>

    @GET("profile/followers")
    suspend fun getFollowers(@Query("page") page: Int, @Query("pageSize") pageSize: Int): Response<BaseResponse<RelationshipPageDto>>

    @POST("profile/following/{userId}")
    suspend fun follow(@Path("userId") userId: String): Response<BaseResponse<RelationshipUserDto>>

    @POST("profile/following/{userId}/cancel")
    suspend fun unfollow(@Path("userId") userId: String): Response<BaseResponse<RelationshipUserDto>>
}

data class RelationshipPageDto(val users: List<RelationshipUserDto>, val nextPage: Int? = null)
data class RelationshipUserDto(
    val userId: String,
    val name: String,
    val initial: String,
    val description: String,
    val avatarColor: Long,
    val isFollowing: Boolean,
)
