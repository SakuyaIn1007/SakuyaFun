package com.sakuya.home.data.repository

import com.sakuya.home.data.datasource.HomeDataSource
import com.sakuya.home.model.ContentItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val homeDataSource: HomeDataSource
) {
    suspend fun getRecommendItems(): Result<List<ContentItem>> {
        return homeDataSource.getRecommendItems()
    }

    suspend fun getNovelItems(): Result<List<ContentItem>> {
        return homeDataSource.getNovelItems()
    }
}
