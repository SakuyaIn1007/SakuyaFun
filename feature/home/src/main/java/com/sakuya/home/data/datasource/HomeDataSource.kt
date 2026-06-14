package com.sakuya.home.data.datasource

import com.sakuya.home.model.ContentItem

interface HomeDataSource {
    suspend fun getRecommendItems(): Result<List<ContentItem>>
    suspend fun getNovelItems(): Result<List<ContentItem>>
}
