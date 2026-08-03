package com.sakuya.catalog.data.repository

import com.sakuya.catalog.data.datasource.CatalogDataSource
import com.sakuya.catalog.model.ContentItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepository @Inject constructor(
    private val catalogDataSource: CatalogDataSource
) {
    suspend fun getRecommendItems(): Result<List<ContentItem>> {
        return catalogDataSource.getRecommendItems()
    }

    suspend fun getNovelItems(): Result<List<ContentItem>> {
        return catalogDataSource.getNovelItems()
    }
}
