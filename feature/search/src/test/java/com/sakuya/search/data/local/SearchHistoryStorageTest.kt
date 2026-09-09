package com.sakuya.search.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

/** 锁定搜索历史的去重、置顶和最大数量，避免 DataStore 中的旧词无限增长。 */
class SearchHistoryStorageTest {
    @Test
    fun newestKeywordMovesToFrontIgnoringCase() {
        assertEquals(listOf("sakuya", "狼与香辛料"), updateSearchHistory(listOf("Sakuya", "狼与香辛料"), " sakuya "))
    }

    @Test
    fun historyKeepsAtMostTenItems() {
        val existing = (1..10).map { "关键词$it" }
        assertEquals(listOf("新关键词") + existing.take(9), updateSearchHistory(existing, "新关键词"))
    }
}
