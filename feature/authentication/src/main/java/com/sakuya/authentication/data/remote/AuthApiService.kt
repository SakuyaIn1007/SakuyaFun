package com.sakuya.authentication.data.remote

import com.google.gson.annotations.SerializedName
import com.sakuya.model.network.BaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<BaseResponse<AuthTokenResponse>>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<BaseResponse<AuthTokenResponse>>
}

data class LoginRequest(
    val account: String,
    val password: String
)

data class RegisterRequest(
    val account: String,
    val password: String,
    val nickname: String
)

data class AuthTokenResponse(
    val token: String? = null,
    @SerializedName("access_token")
    val accessToken: String? = null,
    @SerializedName("jwt_token")
    val jwtToken: String? = null,
    val userId: String? = null,
    val nickname: String? = null
) {
    fun resolvedToken(): String? = token ?: accessToken ?: jwtToken
}
