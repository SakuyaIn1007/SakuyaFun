package com.sakuya.search.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_history")

/**
 * SearchHistoryStorage.kt
 * 职责说明：使用 DataStore 持久化最近搜索词，并保留最近使用顺序。
 * 执行流程：Repository 读取当前列表 -> 去重后将新词移到首位 -> 截取上限 -> 原子写回 DataStore。
 */
@Singleton
class SearchHistoryStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    suspend fun getHistory(): List<String> = decode(context.searchHistoryDataStore.data.first()[HISTORY_KEY])

    suspend fun save(keyword: String) {
        context.searchHistoryDataStore.edit { preferences ->
            preferences[HISTORY_KEY] = gson.toJson(updateSearchHistory(decode(preferences[HISTORY_KEY]), keyword))
        }
    }

    suspend fun clear() {
        context.searchHistoryDataStore.edit { preferences -> preferences.remove(HISTORY_KEY) }
    }

    private fun decode(raw: String?): List<String> = runCatching {
        gson.fromJson<List<String>>(raw ?: "[]", object : TypeToken<List<String>>() {}.type)
    }.getOrDefault(emptyList()).filter(String::isNotBlank).take(MAX_HISTORY_SIZE)

    private companion object {
        val HISTORY_KEY = stringPreferencesKey("recent_keywords")
        const val MAX_HISTORY_SIZE = 10
    }
}

/** 纯函数锁定搜索历史的去重、置顶与数量上限，便于不依赖 Android 环境测试。 */
internal fun updateSearchHistory(existing: List<String>, keyword: String): List<String> {
    val normalized = keyword.trim()
    if (normalized.isEmpty()) return existing.take(10)
    return (listOf(normalized) + existing.filterNot { it.equals(normalized, ignoreCase = true) }).take(10)
}
