package com.sakuya.bookdetail.data.remote

import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface BookDetailApiService {
    @GET("books/{id}")
    suspend fun getBookDetail(
        @Path("id") bookId: String
    ): Response<BaseResponse<BookDetailDto>>
}

data class BookDetailDto(
    val id: String,
    val title: String,
    val author: String,
    val publisher: String,
    val rating: Float,
    val tags: List<String>,
    val description: String,
    val isCollected: Boolean
)
