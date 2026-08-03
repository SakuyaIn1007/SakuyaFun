package com.sakuya.catalog.data.datasource

import com.sakuya.data.catalog.BookCatalogRepository
import com.sakuya.data.catalog.CatalogBook
import com.sakuya.catalog.model.ContentItem
import com.sakuya.catalog.model.RankingItem
import javax.inject.Inject

class MockRankingDataSource @Inject constructor(
    private val catalogRepository: BookCatalogRepository
) : RankingDataSource {

    override suspend fun getRankingItems(): Result<List<RankingItem>> {
        val items = catalogRepository.getRankingBooks().map(CatalogBook::toContentItem)
        return Result.success(items.mapIndexed { index, item ->
            RankingItem(rank = index + 1, item = item)
        })
    }
}

private fun CatalogBook.toContentItem() = ContentItem(
    id = id,
    title = title,
    author = author,
    publisher = publisher,
    rating = rating,
    tags = tags,
    description = description
)
