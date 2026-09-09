package com.sakuya.feed.data.repository

import com.sakuya.feed.data.remote.CreateCommentRequest
import com.sakuya.feed.data.remote.FeedApiService
import com.sakuya.feed.data.remote.FeedCommentDto
import com.sakuya.feed.data.remote.FeedMediaDto
import com.sakuya.feed.data.remote.FeedMediaRequest
import com.sakuya.feed.data.remote.FeedPageDto
import com.sakuya.feed.data.remote.FeedPostDto
import com.sakuya.feed.data.remote.FeedReportRequest
import com.sakuya.feed.data.remote.FeedBooleanActionRequest
import com.sakuya.feed.data.remote.PublishFeedRequest
import com.google.gson.Gson
import com.sakuya.data.local.dao.FeedCacheDao
import com.sakuya.data.local.entity.FeedCacheEntity
import com.sakuya.model.feed.DynamicPost
import com.sakuya.model.feed.FeedComment
import com.sakuya.model.feed.FeedCommentDraft
import com.sakuya.model.feed.FeedDraft
import com.sakuya.model.feed.FeedMediaAttachment
import com.sakuya.model.feed.FeedMediaType
import com.sakuya.model.feed.FeedPage
import com.sakuya.model.feed.FeedReport
import com.sakuya.model.feed.FeedStream
import com.sakuya.model.feed.RelatedNovel
import com.sakuya.model.network.BaseResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response

/**
 * RemoteFeedRepository.kt
 * 职责说明：协调动态 API 和 Room 离线缓存，并完成 DTO 到领域模型的映射。
 * 执行流程：页面先观察 Room；远端分页成功后写缓存，失败时 Room 中已保存的内容继续可用。
 */
class RemoteFeedRepository(
    private val apiService: FeedApiService,
    private val feedCacheDao: FeedCacheDao,
    private val gson: Gson,
) : FeedRepository {
    override fun observeCachedFeed(stream: FeedStream): Flow<List<DynamicPost>> =
        feedCacheDao.observeByStream(stream.name).map { items -> items.map { it.toDomain(gson) } }

    /**
     * 推荐流与关注流共用后端分页端点，只通过 stream 参数区分；关注筛选必须由服务端按当前登录用户执行。
     * 首次分页成功后原子替换对应流缓存，请求失败时不写库，从而保留上一次可用的离线内容。
     */
    override suspend fun getFeed(stream: FeedStream, page: Int, pageSize: Int): Result<FeedPage<DynamicPost>> =
        apiService.getFeed(stream.toApiStream(), page, pageSize).toDomainPage { it.toDomain() }
            .onSuccess { result ->
                val entities = result.items.mapIndexed { index, post ->
                    post.toCacheEntity(stream, page * pageSize + index, gson)
                }
                if (page == 0) feedCacheDao.replaceStream(stream.name, entities) else feedCacheDao.upsertAll(entities)
            }

    /** 作者主页的分页结果直接返回页面状态，避免用第三种 key 污染两条首页流缓存。 */
    override suspend fun getAuthorFeed(authorId: String, page: Int, pageSize: Int): Result<FeedPage<DynamicPost>> =
        apiService.getAuthorFeed(authorId, page, pageSize).toDomainPage { it.toDomain() }

    override suspend fun getPost(postId: String): Result<DynamicPost> =
        apiService.getPost(postId).toDomain { it.toDomain() }

    override suspend fun getComments(postId: String, page: Int, pageSize: Int): Result<FeedPage<FeedComment>> =
        apiService.getComments(postId, page, pageSize).toDomainPage { it.toDomain() }

    override suspend fun publish(draft: FeedDraft): Result<DynamicPost> =
        apiService.publish(draft.toRequest()).toDomain { it.toDomain() }

    override suspend fun update(postId: String, draft: FeedDraft): Result<DynamicPost> =
        apiService.update(postId, draft.toRequest()).toDomain { it.toDomain() }

    override suspend fun delete(postId: String): Result<Unit> = apiService.delete(postId).toDomain()

    override suspend fun createComment(postId: String, draft: FeedCommentDraft): Result<FeedComment> =
        apiService.createComment(postId, CreateCommentRequest(draft.content, draft.replyToCommentId)).toDomain { it.toDomain() }

    override suspend fun setLiked(postId: String, liked: Boolean): Result<DynamicPost> =
        apiService.setLike(postId, FeedBooleanActionRequest(liked)).toDomain { it.toDomain() }

    override suspend fun setFavorited(postId: String, favorited: Boolean): Result<DynamicPost> =
        apiService.setFavorite(postId, FeedBooleanActionRequest(favorited)).toDomain { it.toDomain() }

    override suspend fun report(postId: String, report: FeedReport): Result<Unit> =
        apiService.report(postId, FeedReportRequest(report.reason.name.lowercase(), report.description)).toDomain()
}

/** 显式固定服务端查询值，避免枚举重命名或区域化大小写规则悄悄改变公开 API 契约。 */
internal fun FeedStream.toApiStream(): String = when (this) {
    FeedStream.FOLLOWING -> "following"
    FeedStream.RECOMMENDED -> "recommended"
}

private fun FeedPostDto.toDomain() = DynamicPost(
    id = id, userId = userId, authorName = authorName, authorInitial = authorInitial, authorColor = authorColor,
    title = title, content = content, attachments = attachments.map(FeedMediaDto::toDomain), tags = tags,
    publishedAt = publishedAt, commentCount = commentCount, likeCount = likeCount, favoriteCount = favoriteCount,
    isLiked = isLiked, isFavorited = isFavorited, isMine = isMine,
)

private fun FeedMediaDto.toDomain() = FeedMediaAttachment(
    id = id, type = runCatching { FeedMediaType.valueOf(type.uppercase()) }.getOrDefault(FeedMediaType.IMAGE),
    remoteUrl = url, thumbnailUrl = thumbnailUrl,
)

/** 将复杂展示字段序列化，确保缓存可完全恢复成首页卡片需要的领域模型。 */
private fun DynamicPost.toCacheEntity(stream: FeedStream, position: Int, gson: Gson) = FeedCacheEntity(
    stream.name, id, position, userId, authorName, authorInitial, authorColor, title, content,
    gson.toJson(attachments), gson.toJson(imageColors), gson.toJson(tags), publishedAt,
    relatedNovel?.let(gson::toJson), commentCount, likeCount, favoriteCount, isLiked, isFavorited, isMine,
)

/** 单条缓存解析失败以安全默认值降级，避免旧缓存阻断其他动态的离线展示。 */
private fun FeedCacheEntity.toDomain(gson: Gson) = DynamicPost(
    id = postId, userId = userId, authorName = authorName, authorInitial = authorInitial, authorColor = authorColor,
    title = title, content = content,
    attachments = runCatching { gson.fromJson(attachmentsJson, Array<FeedMediaAttachment>::class.java).toList() }.getOrDefault(emptyList()),
    imageColors = runCatching { gson.fromJson(imageColorsJson, Array<Long>::class.java).toList() }.getOrDefault(emptyList()),
    tags = runCatching { gson.fromJson(tagsJson, Array<String>::class.java).toList() }.getOrDefault(emptyList()),
    publishedAt = publishedAt, relatedNovel = relatedNovelJson?.let { runCatching { gson.fromJson(it, RelatedNovel::class.java) }.getOrNull() },
    commentCount = commentCount, likeCount = likeCount, favoriteCount = favoriteCount, isLiked = isLiked,
    isFavorited = isFavorited, isMine = isMine,
)

private fun FeedCommentDto.toDomain() = FeedComment(
    id = id, postId = postId, authorId = authorId, authorName = authorName, authorInitial = authorInitial,
    content = content, publishedAt = publishedAt, replyToCommentId = replyToCommentId,
    replyToAuthorName = replyToAuthorName, likeCount = likeCount, isLiked = isLiked,
)

private fun FeedDraft.toRequest() = PublishFeedRequest(
    title = title, content = content, topics = topics,
    attachments = attachments.mapNotNull { attachment ->
        attachment.remoteUrl?.let { FeedMediaRequest(attachment.type.name.lowercase(), it) }
    },
    relatedNovelId = relatedNovelId,
)

private fun <T, R> Response<BaseResponse<T>>.toDomain(mapper: (T) -> R): Result<R> =
    if (!isSuccessful) Result.failure(IllegalStateException("HTTP ${code()}"))
    else body()?.toResult()?.map(mapper) ?: Result.failure(IllegalStateException("服务器未返回数据"))

private fun <T> Response<BaseResponse<T>>.toDomain(): Result<T> =
    if (!isSuccessful) Result.failure(IllegalStateException("HTTP ${code()}"))
    else body()?.toResult() ?: Result.failure(IllegalStateException("服务器未返回数据"))

private fun <T, R> Response<BaseResponse<FeedPageDto<T>>>.toDomainPage(mapper: (T) -> R): Result<FeedPage<R>> =
    toDomain { page -> FeedPage(items = page.items.map(mapper), nextPage = page.nextPage) }
