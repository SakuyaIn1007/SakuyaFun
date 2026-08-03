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
}
