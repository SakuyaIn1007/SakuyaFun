package com.sakuya.profile.data.remote

import com.sakuya.model.network.BaseResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ProfileApiService {
    @Multipart
    @POST("profile/upload/avatar")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part
    ): Response<BaseResponse<String>>
}