package com.sakuya.authentication.data.repository

import com.sakuya.authentication.data.remote.AuthApiService
import com.sakuya.authentication.data.remote.LoginRequest
import com.sakuya.authentication.data.remote.RegisterRequest
import com.sakuya.data.local.TokenStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val tokenStorage: TokenStorage
) {
    suspend fun login(account: String, password: String): Result<Unit> {
        return runCatchingAuth {
            val response = apiService.login(LoginRequest(account, password))
            val body = requireNotNull(response.body()) { "服务器返回为空" }
            if (!response.isSuccessful || !body.isSuccess()) {
                error(body.message.ifBlank { "登录失败" })
            }
            val token = body.data?.resolvedToken()
            require(!token.isNullOrBlank()) { "服务器未返回登录凭证" }
            tokenStorage.saveToken(token)
        }
    }

    suspend fun register(account: String, password: String, nickname: String): Result<Unit> {
        return runCatchingAuth {
            val response = apiService.register(RegisterRequest(account, password, nickname))
            val body = requireNotNull(response.body()) { "服务器返回为空" }
            if (!response.isSuccessful || !body.isSuccess()) {
                error(body.message.ifBlank { "注册失败" })
            }
            body.data?.resolvedToken()?.takeIf { it.isNotBlank() }?.let { token ->
                tokenStorage.saveToken(token)
            }
        }
    }

    suspend fun logout() {
        tokenStorage.clearToken()
    }

    private inline fun runCatchingAuth(block: () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
