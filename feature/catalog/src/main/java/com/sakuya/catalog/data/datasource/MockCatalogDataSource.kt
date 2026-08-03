package com.sakuya.catalog.data.datasource

import com.sakuya.data.catalog.BookCatalogRepository
import com.sakuya.data.catalog.CatalogBook
import com.sakuya.catalog.model.ContentItem
import javax.inject.Inject

class MockCatalogDataSource @Inject constructor(
    private val catalogRepository: BookCatalogRepository
) : CatalogDataSource {

    override suspend fun getRecommendItems(): Result<List<ContentItem>> {
        return Result.success(catalogRepository.getRecommendedBooks().map(CatalogBook::toContentItem))
    }

    override suspend fun getNovelItems(): Result<List<ContentItem>> {
        return Result.success(catalogRepository.getNovelBooks().map(CatalogBook::toContentItem))
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
