package com.sakuya.data.local

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Cache
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64
import org.json.JSONObject

@Singleton
/**
 * SessionManager.kt
 * 职责说明：管理当前登录凭证、账号缓存命名空间及退出时需清理的敏感会话数据。
 * 执行流程：登录保存 token/userId；旧会话可从 JWT sub 恢复命名空间；退出清空当前账号的内存态与
 * 内容 HTTP 缓存，但不删除按账号隔离的阅读进度、书签和已下载离线内容。
 */
class SessionManager @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val database: AppDatabase,
    private val updateBadgeState: com.sakuya.data.notification.UpdateBadgeStateStore,
    private val httpCache: Cache,
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
        // OkHttp 缓存键只含 URL 不含 Authorization，内容响应因此会跨账号命中，必须随账号生命周期清空。
        // evictAll 是阻塞式磁盘 I/O，放在事务之外执行，避免长时间持有数据库事务。
        withContext(Dispatchers.IO) { httpCache.evictAll() }
        updateBadgeState.reset()
    }
}
