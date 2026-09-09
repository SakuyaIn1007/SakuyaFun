package com.sakuya.authentication.data.repository

import com.sakuya.authentication.data.remote.AuthApiService
import com.sakuya.authentication.data.remote.LoginRequest
import com.sakuya.authentication.data.remote.RegisterRequest
import com.sakuya.data.local.SessionManager
import com.sakuya.data.notification.PushRegistrationManager
import com.sakuya.data.notification.UpdateBadgeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * AuthRepository.kt
 * 职责说明：完成登录、注册和退出，并建立包含 token 与 userId 的账号会话。
 * 执行流程：认证成功后原子保存账号标识 -> 触发推送注册与应用级数据同步；退出时清除当前会话但保留隔离缓存。
 */
class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val sessionManager: SessionManager,
    private val pushRegistrationManager: PushRegistrationManager,
    private val updateBadges:UpdateBadgeRepository,
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
            val userId = body.data?.userId
            require(!userId.isNullOrBlank()) { "服务器未返回用户标识" }
            sessionManager.startSession(token, userId)
            // 推送注册失败不回滚已成功的登录；下次启动或 FCM Token 刷新会自动补偿。
            pushRegistrationManager.registerCurrentToken()
            updateBadges.refresh()
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
                val userId = body.data?.userId
                require(!userId.isNullOrBlank()) { "服务器未返回用户标识" }
                sessionManager.startSession(token, userId)
                pushRegistrationManager.registerCurrentToken()
                updateBadges.refresh()
            }
        }
    }

    suspend fun logout() {
        pushRegistrationManager.unregister()
        sessionManager.logout()
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
