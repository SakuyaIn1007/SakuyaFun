package com.sakuya.search.data.remote

import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * SearchApiService.kt
 * 职责说明：定义搜索结果、历史与热门关键词的远端协议，隔离服务端字段变化。
 * 执行流程：Repository 调用接口并转换 DTO，UI 和领域层都不直接依赖 Retrofit Response。
 */
interface SearchApiService {
    @GET("search")
    suspend fun search(
        @Query("keyword") keyword: String,
        @Query("type") type: String? = null,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<BaseResponse<SearchPageDto>>

    @GET("search/history")
    suspend fun getHistory(): Response<BaseResponse<List<String>>>

    @GET("search/hot-keywords")
    suspend fun getHotKeywords(): Response<BaseResponse<List<String>>>
}

data class SearchPageDto(val items: List<SearchResultDto>, val nextPage: Int? = null)
data class SearchResultDto(
    val id: String,
    val title: String,
    val summary: String,
    val type: String,
    val tags: List<String> = emptyList(),
)
