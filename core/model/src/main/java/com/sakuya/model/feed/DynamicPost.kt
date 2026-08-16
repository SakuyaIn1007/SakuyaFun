package com.sakuya.model.feed

/**
 * DynamicPost.kt
 * 职责说明：定义动态流、详情页和操作结果共用的领域动态模型。
 * 执行流程：Remote DTO 或本地缓存先映射为本模型，再由 ViewModel 转换为 UiState 供 UI 渲染。
 */
data class DynamicPost(
    val id: String,
    val userId: String = "",
    val authorName: String,
    val authorInitial: String,
    val authorColor: Long,
    val title: String,
    val content: String,
    /** 真实媒体资源；imageColors 仅保留给当前占位卡片的兼容渲染。 */
    val attachments: List<FeedMediaAttachment> = emptyList(),
    val imageColors: List<Long> = emptyList(),
    val tags: List<String> = emptyList(),
    val publishedAt: String = "",
    val relatedNovel: RelatedNovel? = null,
    val commentCount: Int = 0,
    val likeCount: Int = 0,
    val favoriteCount: Int = 0,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    /** 由当前登录用户与 userId 比对后得到；控制更多菜单中的编辑、删除权限。 */
    val isMine: Boolean = false,
)

/** 动态发布和详情展示使用的媒体附件，localUri 用于上传前草稿，remoteUrl 用于已发布内容。 */
data class FeedMediaAttachment(
    val id: String,
    val type: FeedMediaType,
    val localUri: String? = null,
    val remoteUrl: String? = null,
    val thumbnailUrl: String? = null,
    val uploadState: FeedMediaUploadState = FeedMediaUploadState.UPLOADED,
)

enum class FeedMediaType { IMAGE, VIDEO }

enum class FeedMediaUploadState { LOCAL, UPLOADING, UPLOADED, FAILED }

data class RelatedNovel(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
)
