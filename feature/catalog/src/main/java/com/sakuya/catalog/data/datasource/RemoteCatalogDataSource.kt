package com.sakuya.catalog.data.datasource

import com.sakuya.catalog.data.remote.CatalogApiService
import com.sakuya.catalog.model.ContentItem
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class RemoteCatalogDataSource @Inject constructor(
    private val apiService: CatalogApiService
) : CatalogDataSource {
    override suspend fun getRecommendItems(): Result<List<ContentItem>> = request {
        apiService.getRecommendations()
    }

    override suspend fun getNovelItems(): Result<List<ContentItem>> = request {
        apiService.getNovels()
    }
}

internal suspend fun <T> request(
    call: suspend () -> retrofit2.Response<com.sakuya.model.network.BaseResponse<T>>
): Result<T> = try {
    val response = call()
    val body = response.body()
    if (response.isSuccessful && body != null) body.toResult()
    else Result.failure(Exception(body?.message ?: "HTTP ${response.code()}"))
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Result.failure(error)
}
