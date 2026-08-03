package com.sakuya.library.data.remote

import com.sakuya.library.model.LibraryItem
import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LibraryApiService {
    @GET("library")
    suspend fun getLibrary(): Response<BaseResponse<List<LibraryItem>>>

    @POST("library/{bookId}")
    suspend fun addBook(@Path("bookId") bookId: String): Response<BaseResponse<LibraryItem>>

    @DELETE("library/{bookId}")
    suspend fun removeBook(@Path("bookId") bookId: String): Response<BaseResponse<Unit>>
}
