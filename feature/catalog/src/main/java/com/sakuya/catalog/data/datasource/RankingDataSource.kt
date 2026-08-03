package com.sakuya.catalog.data.datasource

import com.sakuya.catalog.model.RankingItem

interface RankingDataSource {
    suspend fun getRankingItems(): Result<List<RankingItem>>
}
