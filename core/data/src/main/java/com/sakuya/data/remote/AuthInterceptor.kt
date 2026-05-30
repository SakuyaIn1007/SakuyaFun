package com.sakuya.data.remote

import com.sakuya.data.local.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor{
    override fun intercept(chain: Interceptor.Chain): Response {
//        获取原始请求
        val originalRequest = chain.request()
//        获取本地Token
        val token = tokenStorage.getTokenBlocking()
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("App-version", "1.0.0")
                .build()
        } else {
            originalRequest
        }
        return chain.proceed(newRequest)
    }
}