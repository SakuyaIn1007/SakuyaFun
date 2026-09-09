package com.sakuya.data.local.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sakuya.data.local.TokenStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenStorage{
    companion object{
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val INSTALLATION_ID_KEY = stringPreferencesKey("reading_installation_id")
    }

//    异步
    override fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }

    override fun getUserId(): Flow<String?> = context.dataStore.data.map { preferences -> preferences[USER_ID_KEY] }

//    同步
//    因为Okhttp的拦截器用不了，所以要用runBlocking同步抠出来
    override fun getTokenBlocking(): String? = runBlocking {
        context.dataStore.data.map { preferences -> preferences[TOKEN_KEY] }.first()
    }

    override fun getUserIdBlocking(): String? = runBlocking {
        context.dataStore.data.map { preferences -> preferences[USER_ID_KEY] }.first()
    }




    override suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    override suspend fun saveSession(token: String, userId: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
        }
    }

    override suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences -> preferences[USER_ID_KEY] = userId }
    }

    override suspend fun getOrCreateInstallationId(): String {
        val existing = context.dataStore.data.map { it[INSTALLATION_ID_KEY] }.first()
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        context.dataStore.edit { preferences ->
            if (preferences[INSTALLATION_ID_KEY].isNullOrBlank()) preferences[INSTALLATION_ID_KEY] = created
        }
        return context.dataStore.data.map { it[INSTALLATION_ID_KEY] }.first() ?: created
    }

    override suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
        }
    }
}
