package com.sakuya.home.data.datasource

import com.sakuya.home.model.RankingItem

interface RankingDataSource {
    suspend fun getRankingItems(): Result<List<RankingItem>>
}
