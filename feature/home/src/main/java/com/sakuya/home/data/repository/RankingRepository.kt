package com.sakuya.home.data.repository

import com.sakuya.home.data.datasource.RankingDataSource
import com.sakuya.home.model.RankingItem
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
