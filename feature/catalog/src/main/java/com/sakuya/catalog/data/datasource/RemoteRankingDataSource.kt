package com.sakuya.catalog.data.datasource

import com.sakuya.catalog.data.remote.CatalogApiService
import com.sakuya.catalog.model.RankingItem
import javax.inject.Inject

class RemoteRankingDataSource @Inject constructor(
    private val apiService: CatalogApiService
) : RankingDataSource {
    override suspend fun getRankingItems(): Result<List<RankingItem>> = request {
        apiService.getRankings()
    }
}
