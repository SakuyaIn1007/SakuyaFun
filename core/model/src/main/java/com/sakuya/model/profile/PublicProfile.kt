package com.sakuya.model.profile

/**
 * PublicProfile.kt
 * 职责说明：他人公开资料与关注关系相关的共享传输模型。
 * 执行流程：feed 的他人主页与 profile 的关注/粉丝列表共用同一批接口，因此 DTO 统一下沉到 core:model，
 * 各业务模块只保留各自的领域映射，避免重复定义漂移。
 */

/** 他人主页：公开资料、三项统计和当前用户的关注状态，字段与 /profiles/{userId} 响应保持一致。 */
data class PublicProfileDto(
    /** 用户唯一标识，用于关注、粉丝和关注列表请求。 */ val userId: String,
    /** 头像地址；为空时页面使用昵称首字母头像。 */ val avatarUrl: String = "",
    /** 公开昵称。 */ val nickname: String,
    /** 公开个性签名。 */ val signature: String? = null,
    /** 该用户主动关注的人数。 */ val followingCount: Long = 0,
    /** 关注该用户的人数。 */ val followerCount: Long = 0,
    /** 该用户已发布的动态总数。 */ val postCount: Long = 0,
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
    val hasUnseenPosts: Boolean = false,
)

/** users 为当前页用户，nextPage 为空时表示没有更多分页数据。 */
data class RelationshipPageDto(val users: List<RelationshipUserDto>, val nextPage: Int? = null)

/** 他人主页发起私信后，只需会话 ID 与标题即可交给共享导航契约打开聊天页。 */
data class PublicAuthorConversationDto(val id: String, val title: String)
