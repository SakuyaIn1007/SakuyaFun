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
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenStorage{
    companion object{
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    }

//    异步
    override fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }

//    同步
//    因为Okhttp的拦截器用不了，所以要用runBlocking同步抠出来
    override fun getTokenBlocking(): String? = runBlocking {
        context.dataStore.data.map { preferences -> preferences[TOKEN_KEY] }.first()
    }




    override suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    override suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }
}