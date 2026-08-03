package com.sakuya.bookdetail.data.repository

import com.sakuya.bookdetail.data.remote.BookDetailApiService
import com.sakuya.bookdetail.data.remote.BookDetailDto
import com.sakuya.data.catalog.BookCatalogRepository
import com.sakuya.data.catalog.CatalogBook
import java.io.IOException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookDetailRepository @Inject constructor(
    private val apiService: BookDetailApiService,
    private val localCatalog: BookCatalogRepository
) {
    suspend fun getBook(bookId: String): Result<BookDetailResult> {
        return try {
            val response = apiService.getBookDetail(bookId)
            val body = response.body()
            val data = body?.data
            if (!response.isSuccessful || body == null || !body.isSuccess() || data == null) {
                Result.failure(Exception(body?.message ?: "加载书籍详情失败（HTTP ${response.code()}）"))
            } else {
                Result.success(BookDetailResult(data.toCatalogBook(), data.isCollected))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            localCatalog.findById(bookId)
                ?.let { Result.success(BookDetailResult(it, isCollected = false)) }
                ?: Result.failure(error)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

data class BookDetailResult(
    val book: CatalogBook,
    val isCollected: Boolean
)

private fun BookDetailDto.toCatalogBook() = CatalogBook(
    id = id,
    title = title,
    author = author,
    publisher = publisher,
    rating = rating,
    tags = tags,
    description = description
)
