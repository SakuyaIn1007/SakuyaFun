package com.sakuya.feed.data.repository

import com.sakuya.feed.data.remote.FeedApiService
import com.sakuya.model.feed.FeedStream
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.GET

/**
 * FeedStreamApiMappingTest.kt
 * 职责说明：锁定 Android 动态分流与后端 /feed stream 查询值的映射契约。
 * 验证范围：关注流必须发送 following，推荐流继续发送 recommended，防止回退为本地模拟筛选。
 */
class FeedStreamApiMappingTest {
    @Test
    fun followingStreamUsesBackendFollowingQueryValue() {
        assertEquals("following", FeedStream.FOLLOWING.toApiStream())
    }

    @Test
    fun recommendedStreamKeepsExistingQueryValue() {
        assertEquals("recommended", FeedStream.RECOMMENDED.toApiStream())
    }

    /** 锁定作者主页的独立路径，避免后续误改为推荐/关注流的查询参数。 */
    @Test
    fun authorFeedUsesIndependentAuthorPath() {
        val method = FeedApiService::class.java.declaredMethods.single { it.name == "getAuthorFeed" }
        val annotation = requireNotNull(method.getAnnotation(GET::class.java))
        assertEquals("feed/authors/{authorId}", annotation.value)
    }
}
