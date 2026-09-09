package com.sakuya.data.notification

import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/** 关注动态提示接口只传递计数与已读游标，不创建通知中心记录。 */
interface UpdateBadgeApiService {
    @GET("feed/following/unseen-summary")
    suspend fun followingSummary(): Response<BaseResponse<FollowingSummaryDto>>

    @PUT("feed/following/read")
    suspend fun markFollowingRead(): Response<BaseResponse<Unit>>

    @PUT("feed/following/authors/{authorId}/read")
    suspend fun markAuthorRead(@Path("authorId") authorId: String): Response<BaseResponse<Unit>>
}

data class FollowingSummaryDto(val postCount:Int,val authorCount:Int)
