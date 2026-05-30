package com.sakuya.data.local

import kotlinx.coroutines.flow.Flow

interface TokenStorage {
    fun getToken(): Flow<String?>

    fun getTokenBlocking(): String?

    suspend fun saveToken(token: String)

    suspend fun clearToken()
}