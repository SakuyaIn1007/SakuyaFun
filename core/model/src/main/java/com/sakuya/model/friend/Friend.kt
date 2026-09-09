package com.sakuya.model.friend

/**
 * Friend.kt
 * 职责说明：定义好友领域模型。
 * 归属说明：下沉至 core:model，供 friend 及关联业务（conversation 会话成员、feed 关注关系）共享。
 */
data class Friend(
    val id: String,
    val name: String,
    val status: String,
    val avatarText: String,
    val isOnline: Boolean = false
)
