package com.sakuya.data.notification

import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.*

/** 通知远端契约；DTO 保留字符串枚举，Repository 负责拒绝未知类型。 */
interface NotificationApiService {
    @GET("notifications") suspend fun list(@Query("page") page: Int, @Query("pageSize") pageSize: Int): Response<BaseResponse<NotificationPageDto>>
    @GET("notifications/unread-count") suspend fun unread(): Response<BaseResponse<UnreadCountDto>>
    @GET("notifications/unread-summary") suspend fun unreadSummary(): Response<BaseResponse<UnreadSummaryDto>>
    @PUT("notifications/{id}/read") suspend fun markRead(@Path("id") id: String): Response<BaseResponse<Unit>>
    @PUT("notifications/read-all") suspend fun markAllRead(): Response<BaseResponse<Unit>>
    @PUT("notifications/read-category") suspend fun markCategoryRead(@Body request: ReadCategoryRequest): Response<BaseResponse<Unit>>
    @POST("notifications/devices") suspend fun register(@Body request: DeviceRequest): Response<BaseResponse<Unit>>
    @DELETE("notifications/devices/{installationId}") suspend fun unregister(@Path("installationId") installationId: String): Response<BaseResponse<Unit>>
    @GET("notifications/preferences") suspend fun preferences(): Response<BaseResponse<PreferenceDto>>
    @PUT("notifications/preferences") suspend fun updatePreferences(@Body request: PreferenceDto): Response<BaseResponse<PreferenceDto>>
}

data class NotificationPageDto(val items: List<NotificationDto>, val nextPage: Int?)
data class NotificationDto(val id:String,val actorId:String,val type:String,val title:String,val content:String,val targetType:String,val targetId:String,val targetUserId:String?,val createdAt:String,val readAt:String?)
data class UnreadCountDto(val count: Int)
data class UnreadSummaryDto(val total:Int,val feedInteraction:Int,val newFollowers:Int,val friendRelation:Int)
data class ReadCategoryRequest(val category:String)
data class DeviceRequest(val installationId:String,val token:String,val platform:String="android")
data class PreferenceDto(val pushEnabled:Boolean,val feedEnabled:Boolean,val friendEnabled:Boolean)
