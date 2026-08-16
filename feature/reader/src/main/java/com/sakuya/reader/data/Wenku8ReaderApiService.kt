package com.sakuya.reader.data

import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** 远端章节只经由应用后端加载，避免客户端持有第三方站点访问逻辑。 */
interface Wenku8ReaderApiService {
    /** 全文仅由 Spring Boot 网关代理，响应中的正文只在当前阅读会话使用。 */
    @GET("wenku8/novels/{novelId}/full-content")
    suspend fun fullContent(@Path("novelId") novelId: String): Response<BaseResponse<Wenku8FullContentDto>>

    @GET("wenku8/chapters/{chapterId}/content")
    suspend fun content(@Path("chapterId") chapterId: String, @Query("novelId") novelId: String): Response<BaseResponse<Wenku8ReaderContentDto>>
}

/** 后端全文网关的稳定 DTO；offset=-1 时客户端不得尝试定位，应降级为单章正文。 */
data class Wenku8FullContentDto(
    val novelId: String? = null,
    val title: String? = null,
    val content: String? = null,
    val chapters: List<Wenku8ChapterAnchorDto> = emptyList(),
)

data class Wenku8ChapterAnchorDto(
    val chapterId: String? = null,
    val title: String? = null,
    val volumeTitle: String? = null,
    val offset: Int = -1,
)

/**
 * Python 适配服务当前不返回 chapter title，且上游异常响应可能缺字段；所有文本字段在网络边界允许为空。
 */
data class Wenku8ReaderContentDto(
    val novelId: String? = null,
    val chapterId: String? = null,
    val title: String? = null,
    val content: String? = null,
)
