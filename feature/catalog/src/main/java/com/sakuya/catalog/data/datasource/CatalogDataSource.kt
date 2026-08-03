package com.sakuya.catalog.data.datasource

import com.sakuya.catalog.model.ContentItem

interface CatalogDataSource {
    suspend fun getRecommendItems(): Result<List<ContentItem>>
    suspend fun getNovelItems(): Result<List<ContentItem>>
}
