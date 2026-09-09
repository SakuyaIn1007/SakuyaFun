package com.sakuya.data.content

import com.sakuya.data.BuildConfig
import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ContentApiService.kt
 * 职责说明：定义 Android 唯一的来源无关内容协议，搜索、详情和阅读模块共用同一实例。
 * 执行流程：Feature Repository 传入稳定 bookId/chapterId -> Spring Boot 从内容库读取 -> DTO 映射为页面状态。
 */
interface ContentApiService {
    @GET("content/novels")
    suspend fun search(
        @Query("keyword") keyword: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<BaseResponse<ContentNovelPageDto>>

    @GET("content/novels/{bookId}")
    suspend fun novel(@Path("bookId") bookId: String): Response<BaseResponse<ContentNovelDto>>

    @GET("content/novels/{bookId}/chapters")
    suspend fun chapters(@Path("bookId") bookId: String): Response<BaseResponse<ContentChapterIndexDto>>

    @GET("content/chapters/{chapterId}")
    suspend fun chapter(
        @Path("chapterId") chapterId: String,
        @Query("bookId") bookId: String,
    ): Response<BaseResponse<ContentChapterContentDto>>

    @GET("content/novels/{bookId}/full-content")
    suspend fun fullContent(@Path("bookId") bookId: String): Response<BaseResponse<ContentFullDocumentDto>>
}

/**
 * 将后端 DTO 中的封面地址转换为 Coil 可请求地址。
 * 内容库通常返回站内相对路径，但部署迁移期间也允许返回完整 CDN 地址，后者不能再次拼接 API Host。
 */
fun resolveContentUrl(url: String?): String? = url
    ?.trim()
    ?.takeIf(String::isNotEmpty)
    ?.let { value ->
        if (value.startsWith("http://") || value.startsWith("https://")) value
        else BuildConfig.API_BASE_URL.trimEnd('/') + "/" + value.trimStart('/')
    }

data class ContentNovelPageDto(val items: List<ContentNovelDto> = emptyList(), val nextPage: Int? = null)
data class ContentNovelDto(
    val id: String,
    val title: String,
    val author: String = "未知作者",
    val description: String = "",
    val status: String = "",
    val tags: List<String> = emptyList(),
    val copyright: Boolean = false,
    val coverUrl: String? = null,
    val rightsStatus: String = "UNKNOWN",
)
data class ContentChapterIndexDto(val bookId: String? = null, val volumes: List<ContentVolumeDto> = emptyList())
data class ContentVolumeDto(val id: String = "", val title: String, val chapters: List<ContentChapterDto> = emptyList())
data class ContentChapterDto(val id: String, val title: String, val order: Int = 0)
data class ContentChapterContentDto(
    val novelId: String? = null,
    val chapterId: String? = null,
    val title: String? = null,
    val content: String? = null,
)
data class ContentFullDocumentDto(
    val novelId: String? = null,
    val title: String? = null,
    val content: String? = null,
    val chapters: List<ContentChapterAnchorDto> = emptyList(),
)
data class ContentChapterAnchorDto(
    val chapterId: String? = null,
    val title: String? = null,
    val volumeTitle: String? = null,
    val offset: Int = -1,
)
