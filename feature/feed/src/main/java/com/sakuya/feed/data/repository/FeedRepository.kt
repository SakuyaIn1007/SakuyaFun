package com.sakuya.feed.data.repository

import com.sakuya.model.feed.DynamicPost
import com.sakuya.model.feed.FeedComment
import com.sakuya.model.feed.FeedCommentDraft
import com.sakuya.model.feed.FeedDraft
import com.sakuya.model.feed.FeedPage
import com.sakuya.model.feed.FeedStream
import com.sakuya.model.feed.FeedReport
import com.sakuya.model.feed.FeedReportReason
import com.sakuya.model.feed.RelatedNovel
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 动态数据访问契约。
 *
 * ViewModel 只依赖这一层；当前由内存实现提供可运行闭环，接入后端时替换实现即可，无需改动 UI。
 */
interface FeedRepository {
    fun observeCachedFeed(stream: FeedStream): Flow<List<DynamicPost>>
    suspend fun getFeed(stream: FeedStream, page: Int, pageSize: Int): Result<FeedPage<DynamicPost>>
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

/**
 * 第一阶段的内存数据源。
 *
 * 它模拟远端分页和发布结果，并作为首页、详情和发布页的单一数据来源，确保发布后的内容能即时回到列表顶部。
 */
object InMemoryFeedRepository : FeedRepository {
    override fun observeCachedFeed(stream: FeedStream): Flow<List<DynamicPost>> = kotlinx.coroutines.flow.flowOf(emptyList())
    private val posts = mutableListOf(
        DynamicPost(
            id = "recommended-1",
            userId = "user-moon",
            authorName = "月见草",
            authorInitial = "月",
            authorColor = 0xFF8B7BBE,
            title = "雨天读完《山茶文具店》",
            content = "像收到一封温柔的信。书里那些替人写下的话，让这个潮湿的下午也慢慢安静下来。",
            imageColors = listOf(0xFFC9AD8D, 0xFF7D9B94, 0xFFB98773),
            tags = listOf("#读书笔记", "#治愈系"),
            publishedAt = "2小时前",
            relatedNovel = RelatedNovel("camellia-stationery-shop", "山茶文具店", "小川糸", "一间替人书写心意的小店。"),
            commentCount = 3,
            likeCount = 76,
        ),
        DynamicPost(
            id = "recommended-2",
            userId = "user-forest",
            authorName = "林间风",
            authorInitial = "林",
            authorColor = 0xFF5D8C77,
            title = "给八月列了一张待读清单",
            content = "想把阅读的速度放慢一点，留些空白给散步和发呆。你们最近在读什么？",
            imageColors = listOf(0xFF6E879A, 0xFFD5A46D),
            tags = listOf("#待读书单", "#八月阅读"),
            publishedAt = "昨天",
            commentCount = 32,
            likeCount = 104,
        ),
    )

    private val comments = mutableListOf(
        FeedComment("comment-1", "recommended-1", authorName = "林间风", authorInitial = "林", content = "这本书也在我的待读清单里，读完你的分享更想打开它了。", publishedAt = "2小时前"),
        FeedComment("comment-2", "recommended-1", authorName = "南枝", authorInitial = "南", content = "“替人写信”这个设定真的很动人，感谢推荐。", publishedAt = "1小时前"),
        FeedComment("comment-3", "recommended-1", authorName = "青柠", authorInitial = "青", content = "雨天和这本书太配了。", publishedAt = "36分钟前"),
    )

    override suspend fun getFeed(
        stream: FeedStream,
        page: Int,
        pageSize: Int,
    ): Result<FeedPage<DynamicPost>> = runCatching {
        // 关注流只返回已关注作者的内容；接入远端后由 /feed/following 与 /feed/recommended 分别承载。
        val source = when (stream) {
            FeedStream.FOLLOWING -> posts.filter { it.id == "recommended-1" }
            FeedStream.RECOMMENDED -> posts
        }
        val start = page * pageSize
        val pageItems = source.drop(start).take(pageSize)
        FeedPage(pageItems, nextPage = (page + 1).takeIf { start + pageItems.size < source.size })
    }

    override suspend fun getPost(postId: String): Result<DynamicPost> = runCatching {
        posts.firstOrNull { it.id == postId } ?: error("动态不存在或已被删除")
    }

    override suspend fun getComments(
        postId: String,
        page: Int,
        pageSize: Int,
    ): Result<FeedPage<FeedComment>> = runCatching {
        val postComments = comments.filter { it.postId == postId }
        val start = page * pageSize
        val pageItems = postComments.drop(start).take(pageSize)
        FeedPage(pageItems, nextPage = (page + 1).takeIf { start + pageItems.size < postComments.size })
    }

    override suspend fun publish(draft: FeedDraft): Result<DynamicPost> = runCatching {
        require(draft.content.isNotBlank()) { "动态内容不能为空" }
        DynamicPost(
            id = UUID.randomUUID().toString(),
            userId = "current-user",
            authorName = "我",
            authorInitial = "我",
            authorColor = 0xFF6D7FB8,
            title = draft.title.ifBlank { draft.content.lineSequence().first().take(30) },
            content = draft.content.trim(),
            tags = draft.topics,
            publishedAt = "刚刚",
            isMine = true,
        ).also(posts::addFirst)
    }

    /** 编辑仅允许当前用户的动态；成功后替换列表中的同一项，保证刷新后内容一致。 */
    override suspend fun update(postId: String, draft: FeedDraft): Result<DynamicPost> = runCatching {
        val index = posts.indexOfFirst { it.id == postId && it.isMine }
        require(index >= 0) { "无权编辑该动态" }
        posts[index].copy(title = draft.title.ifBlank { draft.content.take(30) }, content = draft.content, tags = draft.topics).also { posts[index] = it }
    }

    /** 删除同样由数据层执行权限校验，并同步移除关联评论。 */
    override suspend fun delete(postId: String): Result<Unit> = runCatching {
        val post = posts.firstOrNull { it.id == postId && it.isMine } ?: error("无权删除该动态")
        posts.remove(post)
        comments.removeAll { it.postId == postId }
    }

    /** 提交评论后同步增加动态评论数，令详情与时间线读取同一个数据快照。 */
    override suspend fun createComment(postId: String, draft: FeedCommentDraft): Result<FeedComment> = runCatching {
        require(draft.content.isNotBlank()) { "评论内容不能为空" }
        val postIndex = posts.indexOfFirst { it.id == postId }
        require(postIndex >= 0) { "动态不存在或已被删除" }
        FeedComment(
            id = UUID.randomUUID().toString(), postId = postId, authorId = "current-user", authorName = "我",
            authorInitial = "我", content = draft.content.trim(), publishedAt = "刚刚", replyToCommentId = draft.replyToCommentId,
        ).also { comment ->
            comments.add(comment)
            posts[postIndex] = posts[postIndex].copy(commentCount = posts[postIndex].commentCount + 1)
        }
    }

    /** 点赞与收藏均返回更新后的动态，调用方可用该结果原子替换当前 UI 数据。 */
    override suspend fun setLiked(postId: String, liked: Boolean): Result<DynamicPost> = updateEngagement(postId) { post ->
        if (post.isLiked == liked) post else post.copy(isLiked = liked, likeCount = (post.likeCount + if (liked) 1 else -1).coerceAtLeast(0))
    }

    override suspend fun setFavorited(postId: String, favorited: Boolean): Result<DynamicPost> = updateEngagement(postId) { post ->
        if (post.isFavorited == favorited) post else post.copy(isFavorited = favorited, favoriteCount = (post.favoriteCount + if (favorited) 1 else -1).coerceAtLeast(0))
    }

    override suspend fun report(postId: String, report: FeedReport): Result<Unit> = runCatching {
        require(posts.any { it.id == postId }) { "动态不存在或已被删除" }
        require(report.reason != FeedReportReason.OTHER || report.description.isNotBlank()) { "请选择举报原因或填写说明" }
    }

    private fun updateEngagement(postId: String, transform: (DynamicPost) -> DynamicPost): Result<DynamicPost> = runCatching {
        val index = posts.indexOfFirst { it.id == postId }
        require(index >= 0) { "动态不存在或已被删除" }
        transform(posts[index]).also { posts[index] = it }
    }
}
