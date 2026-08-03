package com.sakuya.catalog.data.repository

import com.sakuya.catalog.data.datasource.RankingDataSource
import com.sakuya.catalog.model.RankingItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RankingRepository @Inject constructor(
    private val rankingDataSource: RankingDataSource
) {
    suspend fun getRankingItems(): Result<List<RankingItem>> {
        return rankingDataSource.getRankingItems()
    }
}
