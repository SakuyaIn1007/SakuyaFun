package com.sakuya.model.feed

/**
 * 动态评论的领域模型。
 *
 * 数据层负责返回该模型，详情页只负责按评论人、内容与发布时间进行展示，避免直接依赖网络 DTO。
 */
data class FeedComment(
    val id: String,
    val postId: String,
    val authorId: String = "",
    val authorName: String,
    val authorInitial: String,
    val content: String,
    val publishedAt: String,
    val replyToCommentId: String? = null,
    val replyToAuthorName: String? = null,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
)

/** 分页加载动态或评论时返回的统一结果。 */
data class FeedPage<T>(
    val items: List<T>,
    val nextPage: Int? = null,
)

/**
 * 发布页在提交前使用的草稿数据。
 *
 * 第一阶段只支持文字；图片、话题和关联书籍可在不改变发布流程的前提下逐步扩展。
 */
data class FeedDraft(
    val title: String = "",
    val content: String,
    val topics: List<String> = emptyList(),
    val attachments: List<FeedMediaAttachment> = emptyList(),
    val relatedNovelId: String? = null,
)

/** 评论提交前的领域请求，支持对动态直接评论或回复某一条评论。 */
data class FeedCommentDraft(
    val content: String,
    val replyToCommentId: String? = null,
)

/** 举报请求单独建模，避免 UI 将显示文本直接作为网络参数传递。 */
data class FeedReport(
    val reason: FeedReportReason,
    val description: String = "",
)

enum class FeedReportReason { SPAM, ABUSE, COPYRIGHT, OTHER }

/** 首页的两条独立内容流，Repository 根据该类型选择对应数据源或后端接口。 */
enum class FeedStream {
    FOLLOWING,
    RECOMMENDED,
}
