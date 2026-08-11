package com.sakuya.catalog.data.remote

import com.sakuya.catalog.model.ContentItem
import com.sakuya.catalog.model.RankingItem
import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.GET

interface CatalogApiService {
    @GET("home/recommendations")
    suspend fun getRecommendations(): Response<BaseResponse<List<ContentItem>>>

    @GET("home/novels")
    suspend fun getNovels(): Response<BaseResponse<List<ContentItem>>>

    @GET("home/rankings")
    suspend fun getRankings(): Response<BaseResponse<List<RankingItem>>>

    /** 已公布轻小说更新时间表；每条记录携带小说摘要、日期、卷信息与推荐标记。 */
    @GET("novels/releases")
    suspend fun getPublishedReleases(): Response<BaseResponse<List<NovelReleaseDto>>>
}

data class NovelReleaseDto(
    val novel: ContentItem,
    val releaseDate: String,
    val volumeName: String,
    val isRecommended: Boolean,
)
