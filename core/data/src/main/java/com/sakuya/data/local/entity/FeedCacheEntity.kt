package com.sakuya.data.local.entity

import androidx.room.Entity

/**
 * FeedCacheEntity.kt
 * 职责说明：保存首页单条动态在指定分流中的离线展示快照。
 * 执行流程：远端分页成功后由动态仓库映射并写入本表；Room 按 stream 与 position 返回稳定顺序，
 * 使页面在无网络或进程重建后仍能先展示上一次成功内容。
 */
@Entity(tableName = "feed_cache", primaryKeys = ["stream", "postId"])
data class FeedCacheEntity(
    val stream: String,
    val postId: String,
    val position: Int,
    val userId: String,
    val authorName: String,
    val authorInitial: String,
    val authorColor: Long,
    val title: String,
    val content: String,
    val attachmentsJson: String,
    val imageColorsJson: String,
    val tagsJson: String,
    val publishedAt: String,
    val relatedNovelJson: String?,
    val commentCount: Int,
    val likeCount: Int,
    val favoriteCount: Int,
    val isLiked: Boolean,
    val isFavorited: Boolean,
    val isMine: Boolean,
)
