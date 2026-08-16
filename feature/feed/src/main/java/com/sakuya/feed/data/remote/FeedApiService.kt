package com.sakuya.feed.data.remote

import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * FeedApiService.kt
 * 职责说明：声明动态领域的远端接口与传输 DTO，不向 UI 泄漏 Retrofit 或服务端字段。
 * 执行流程：Repository 调用本接口 -> 校验 BaseResponse -> 映射 DTO 为 core:model 中的领域模型。
 */
interface FeedApiService {
    /** 他人主页：返回公开资料、三项统计和当前用户的关注状态。 */
    @GET("profiles/{userId}")
    suspend fun getPublicProfile(@Path("userId") userId: String): Response<BaseResponse<PublicProfileDto>>

    /** 将当前登录用户与目标用户之间的关注状态更新为已关注。 */
    @POST("profiles/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: String): Response<BaseResponse<RelationshipUserDto>>

    /** 取消当前登录用户对目标用户的关注。 */
    @DELETE("profiles/{userId}/follow")
    suspend fun unfollowUser(@Path("userId") userId: String): Response<BaseResponse<RelationshipUserDto>>

    /** 分页读取目标用户的关注列表。 */
    @GET("profiles/{userId}/following")
    suspend fun getFollowingUsers(@Path("userId") userId: String, @Query("page") page: Int, @Query("pageSize") pageSize: Int): Response<BaseResponse<RelationshipPageDto>>

    /** 分页读取目标用户的粉丝列表。 */
    @GET("profiles/{userId}/followers")
    suspend fun getFollowerUsers(@Path("userId") userId: String, @Query("page") page: Int, @Query("pageSize") pageSize: Int): Response<BaseResponse<RelationshipPageDto>>

    @GET("feed")
    suspend fun getFeed(
        @Query("stream") stream: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<BaseResponse<FeedPageDto<FeedPostDto>>>

    @GET("feed/{postId}")
    suspend fun getPost(@Path("postId") postId: String): Response<BaseResponse<FeedPostDto>>

    @GET("feed/{postId}/comments")
    suspend fun getComments(
        @Path("postId") postId: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<BaseResponse<FeedPageDto<FeedCommentDto>>>

    @POST("feed")
    suspend fun publish(@Body request: PublishFeedRequest): Response<BaseResponse<FeedPostDto>>

    @PUT("feed/{postId}")
    suspend fun update(
        @Path("postId") postId: String,
        @Body request: PublishFeedRequest,
    ): Response<BaseResponse<FeedPostDto>>

    @DELETE("feed/{postId}")
    suspend fun delete(@Path("postId") postId: String): Response<BaseResponse<Unit>>

    @POST("feed/{postId}/comments")
    suspend fun createComment(
        @Path("postId") postId: String,
        @Body request: CreateCommentRequest,
    ): Response<BaseResponse<FeedCommentDto>>

    @POST("feed/{postId}/like")
    suspend fun setLike(
        @Path("postId") postId: String,
        @Body request: FeedBooleanActionRequest,
    ): Response<BaseResponse<FeedPostDto>>

    @POST("feed/{postId}/favorite")
    suspend fun setFavorite(
        @Path("postId") postId: String,
        @Body request: FeedBooleanActionRequest,
    ): Response<BaseResponse<FeedPostDto>>

    @POST("feed/{postId}/report")
    suspend fun report(
        @Path("postId") postId: String,
        @Body request: FeedReportRequest,
    ): Response<BaseResponse<Unit>>
}

/**
 * PublicProfileDto.kt
 * 职责说明：承接他人主页接口，字段与服务端 /profiles/{userId} 响应保持一致。
 * 执行流程：PublicAuthorViewModel 拉取 DTO -> 转为页面状态 -> Compose 根据状态渲染三项统计。
 */
data class PublicProfileDto(
    /** 用户唯一标识，用于关注、粉丝和关注列表请求。 */ val userId: String,
    /** 头像地址；为空时页面使用昵称首字母头像。 */ val avatarUrl: String = "",
    /** 公开昵称。 */ val nickname: String,
    /** 公开个性签名。 */ val signature: String? = null,
    /** 该用户主动关注的人数。 */ val followingCount: Long = 0,
    /** 关注该用户的人数。 */ val followerCount: Long = 0,
    /** 该用户收到的动态点赞与收藏总数。 */ val likesAndFavoritesCount: Long = 0,
    /** 当前登录用户是否已关注该用户。 */ val isFollowing: Boolean = false,
)

/** 关注操作的最小返回结构；列表页和主页底部按钮共用。 */
data class RelationshipUserDto(
    val userId: String,
    val name: String = "",
    val initial: String = "?",
    val description: String? = null,
    val avatarColor: Long = 0L,
    val isFollowing: Boolean,
)

/** users 为当前页用户，nextPage 为空时表示没有更多分页数据。 */
data class RelationshipPageDto(val users: List<RelationshipUserDto>, val nextPage: Int? = null)

data class FeedPageDto<T>(val items: List<T>, val nextPage: Int? = null)

data class FeedPostDto(
    val id: String,
    val userId: String,
    val authorName: String,
    val authorInitial: String,
    val authorColor: Long,
    val title: String,
    val content: String,
    val attachments: List<FeedMediaDto> = emptyList(),
    val tags: List<String> = emptyList(),
    val publishedAt: String,
    val commentCount: Int = 0,
    val likeCount: Int = 0,
    val favoriteCount: Int = 0,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val isMine: Boolean = false,
)

data class FeedMediaDto(
    val id: String,
    val type: String,
    val url: String,
    val thumbnailUrl: String? = null,
)

data class FeedCommentDto(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorInitial: String,
    val content: String,
    val publishedAt: String,
    val replyToCommentId: String? = null,
    val replyToAuthorName: String? = null,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
)

data class PublishFeedRequest(
    val title: String,
    val content: String,
    val topics: List<String>,
    val attachments: List<FeedMediaRequest>,
    val relatedNovelId: String? = null,
)

data class FeedMediaRequest(val type: String, val url: String)
data class CreateCommentRequest(val content: String, val replyToCommentId: String? = null)
data class FeedBooleanActionRequest(val enabled: Boolean)
data class FeedReportRequest(val reason: String, val description: String)
