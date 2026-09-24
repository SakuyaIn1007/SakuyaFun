package com.sakuya.data.feed

import com.sakuya.model.feed.DynamicPost
import com.sakuya.model.feed.FeedPage

/** 职责说明：作者动态分页的跨模块窄契约，供个人主页读取「我的动态」。 */
interface AuthorFeedSource {
    suspend fun getAuthorFeed(authorId: String, page: Int, pageSize: Int): Result<FeedPage<DynamicPost>>
}
