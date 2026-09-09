package com.sakuya.profile.data.remote

import com.sakuya.model.network.BaseResponse
import com.sakuya.model.profile.RelationshipPageDto
import com.sakuya.model.profile.RelationshipUserDto
import retrofit2.Response
import retrofit2.http.DELETE
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
    @GET("profiles/{userId}/following")
    suspend fun getFollowing(
        @Path("userId") userId: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<BaseResponse<RelationshipPageDto>>

    @GET("profiles/{userId}/followers")
    suspend fun getFollowers(
        @Path("userId") userId: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<BaseResponse<RelationshipPageDto>>

    @POST("profiles/{userId}/follow")
    suspend fun follow(@Path("userId") userId: String): Response<BaseResponse<RelationshipUserDto>>

    @DELETE("profiles/{userId}/follow")
    suspend fun unfollow(@Path("userId") userId: String): Response<BaseResponse<RelationshipUserDto>>
}
