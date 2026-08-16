package com.sakuya.search.data.wenku8

import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Wenku8ApiService.kt
 * 职责说明：定义 Android 到受认证 Spring Boot Wenku8 网关的协议，不直接访问 Python 适配服务。
 * 执行流程：Repository 调用本接口 -> Retrofit 附加应用 JWT -> 后端验证后转发内网适配服务。
 */
interface Wenku8ApiService {
    @GET("wenku8/novels/search") suspend fun search(@Query("keyword") keyword: String, @Query("page") page: Int): Response<BaseResponse<Wenku8SearchPageDto>>
    @GET("wenku8/novels/{id}") suspend fun novel(@Path("id") id: String): Response<BaseResponse<Wenku8NovelDto>>
    @GET("wenku8/novels/{id}/chapters") suspend fun chapters(@Path("id") id: String): Response<BaseResponse<Wenku8ChapterIndexDto>>
    @GET("wenku8/chapters/{id}/content") suspend fun content(@Path("id") chapterId: String, @Query("novelId") novelId: String): Response<BaseResponse<Wenku8ChapterContentDto>>
}

data class Wenku8SearchPageDto(val items: List<Wenku8NovelDto> = emptyList(), val nextPage: Int? = null)
data class Wenku8NovelDto(val id: String, val title: String, val author: String = "未知作者", val description: String = "", val status: String = "", val tags: List<String> = emptyList(), val copyright: Boolean = false)
data class Wenku8ChapterIndexDto(val volumes: List<Wenku8VolumeDto> = emptyList())
data class Wenku8VolumeDto(val id: String = "", val title: String, val chapters: List<Wenku8ChapterDto> = emptyList())
data class Wenku8ChapterDto(val id: String, val title: String, val order: Int = 0)
data class Wenku8ChapterContentDto(val novelId: String, val chapterId: String, val title: String = "", val content: String)
