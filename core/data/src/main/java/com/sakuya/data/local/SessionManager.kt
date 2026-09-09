package com.sakuya.data.local

import androidx.room.withTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64
import org.json.JSONObject

@Singleton
/**
 * SessionManager.kt
 * 职责说明：管理当前登录凭证、账号缓存命名空间及退出时需清理的敏感会话数据。
 * 执行流程：登录保存 token/userId；旧会话可从 JWT sub 恢复命名空间；退出只清当前账号指针，不删除隔离的阅读缓存。
 */
class SessionManager @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val database: AppDatabase,
    private val updateBadgeState: com.sakuya.data.notification.UpdateBadgeStateStore,
) {
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired = _sessionExpired.asStateFlow()

    fun getTokenBlocking(): String? = tokenStorage.getTokenBlocking()

    /** 登录时同时保存用户 ID，使离线缓存始终按账号隔离。 */
    suspend fun startSession(token: String, userId: String) {
        clearAccountCache()
        tokenStorage.saveSession(token, userId)
        _sessionExpired.value = false
    }

    /**
     * 兼容升级前只保存 JWT 的会话：仅解析已由服务端签发的 sub 作为本地缓存命名空间，
     * 真实权限仍由服务端验签决定，解析失败时不允许同步账号数据。
     */
    suspend fun currentUserId(): String? {
        tokenStorage.getUserIdBlocking()?.takeIf { it.isNotBlank() }?.let { return it }
        val token = tokenStorage.getTokenBlocking() ?: return null
        val subject = runCatching {
            val payload = token.split('.').getOrNull(1) ?: return@runCatching null
            val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            JSONObject(decoded).optString("sub").takeIf { it.isNotBlank() }
        }.getOrNull() ?: return null
        tokenStorage.saveUserId(subject)
        return subject
    }

    suspend fun installationId(): String = tokenStorage.getOrCreateInstallationId()

    suspend fun expireSession() {
        tokenStorage.clearToken()
        _sessionExpired.value = true
        clearAccountCache()
    }

    suspend fun logout() = expireSession()

    private suspend fun clearAccountCache() {
        database.withTransaction {
            database.chatMessageDao().clearAll()
            database.conversationDao().clearAll()
            database.userDao().clearAllUsers()
            database.notificationDao().clearAll()
        }
        updateBadgeState.reset()
    }
}
