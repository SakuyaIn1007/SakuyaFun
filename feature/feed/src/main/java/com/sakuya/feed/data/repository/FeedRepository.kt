package com.sakuya.feed.data.repository

import com.sakuya.model.feed.DynamicPost
import com.sakuya.model.feed.FeedComment
import com.sakuya.model.feed.FeedCommentDraft
import com.sakuya.model.feed.FeedDraft
import com.sakuya.model.feed.FeedPage
import com.sakuya.model.feed.FeedStream
import com.sakuya.model.feed.FeedReport
import kotlinx.coroutines.flow.Flow

/**
 * FeedRepository.kt
 * 职责说明：定义动态流、详情、评论与互动操作的数据访问契约。
 * 数据流：ViewModel 传入推荐流或关注流 -> 远端实现请求对应的 /feed 分页数据 -> 成功结果写入独立 Room 缓存。
 * 错误处理：远端失败通过 Result 返回，调用方可继续展示 observeCachedFeed 提供的离线快照。
 */
interface FeedRepository {
    fun observeCachedFeed(stream: FeedStream): Flow<List<DynamicPost>>
    suspend fun getFeed(stream: FeedStream, page: Int, pageSize: Int): Result<FeedPage<DynamicPost>>
    /** 作者动态是主页独立分页数据，不写入推荐/关注流 Room 缓存。 */
    suspend fun getAuthorFeed(authorId: String, page: Int, pageSize: Int): Result<FeedPage<DynamicPost>>
    suspend fun getPost(postId: String): Result<DynamicPost>
    suspend fun getComments(postId: String, page: Int, pageSize: Int): Result<FeedPage<FeedComment>>
    suspend fun publish(draft: FeedDraft): Result<DynamicPost>
    suspend fun update(postId: String, draft: FeedDraft): Result<DynamicPost>
    suspend fun delete(postId: String): Result<Unit>
    suspend fun createComment(postId: String, draft: FeedCommentDraft): Result<FeedComment>
    suspend fun setLiked(postId: String, liked: Boolean): Result<DynamicPost>
    suspend fun setFavorited(postId: String, favorited: Boolean): Result<DynamicPost>
    suspend fun report(postId: String, report: FeedReport): Result<Unit>
}
