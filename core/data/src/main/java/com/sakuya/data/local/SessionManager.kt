package com.sakuya.data.local

import androidx.room.withTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val database: AppDatabase
) {
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired = _sessionExpired.asStateFlow()

    fun getTokenBlocking(): String? = tokenStorage.getTokenBlocking()

    suspend fun startSession(token: String) {
        clearAccountCache()
        tokenStorage.saveToken(token)
        _sessionExpired.value = false
    }

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
        }
    }
}
