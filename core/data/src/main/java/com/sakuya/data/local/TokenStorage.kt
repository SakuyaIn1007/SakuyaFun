package com.sakuya.data.local

import kotlinx.coroutines.flow.Flow

interface TokenStorage {
    fun getToken(): Flow<String?>

    fun getUserId(): Flow<String?>

    fun getTokenBlocking(): String?

    fun getUserIdBlocking(): String?

    suspend fun saveToken(token: String)

    suspend fun saveSession(token: String, userId: String)

    suspend fun saveUserId(userId: String)

    suspend fun getOrCreateInstallationId(): String

    suspend fun clearToken()
}
