package com.sakuya.profile.model

/**
 * RelationshipUser.kt
 * 职责说明：定义关注和粉丝列表共用的领域用户，以及分页关系结果。
 * 执行流程：RelationshipRepository 将远端 DTO 映射为本模型，ViewModel 再将关注操作状态写入 UiState。
 */
data class RelationshipUser(
    val userId: String,
    val name: String,
    val initial: String,
    val description: String,
    val avatarColor: Long,
    val isFollowing: Boolean,
)

data class RelationshipPage(
    val users: List<RelationshipUser>,
    val nextPage: Int? = null,
)

enum class RelationshipListType { FOLLOWING, FOLLOWERS }
