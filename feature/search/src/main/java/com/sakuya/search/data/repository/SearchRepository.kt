package com.sakuya.search.data.repository

import com.sakuya.model.search.SearchContentType
import com.sakuya.model.search.SearchPage
import com.sakuya.model.search.SearchQuery
import com.sakuya.model.search.SearchResult

/**
 * SearchRepository.kt
 * 职责说明：统一搜索、搜索历史和热门关键词的数据访问入口。
 * 执行流程：ViewModel 仅依赖该契约；切换本地缓存或远端实现时无需修改 Compose 页面。
 */
interface SearchRepository {
    suspend fun search(query: SearchQuery, page: Int, pageSize: Int): Result<SearchPage>
    suspend fun getHistory(): Result<List<String>>
    suspend fun getHotKeywords(): Result<List<String>>
    suspend fun saveHistory(keyword: String): Result<Unit>
    suspend fun clearHistory(): Result<Unit>
}

/** 可运行的默认数据源，同时覆盖筛选、分页和历史写入流程。 */
object InMemorySearchRepository : SearchRepository {
    private val history = mutableListOf("孤独摇滚", "阅读记录", "紫罗兰永恒花园")
    private val allResults = listOf(
        SearchResult("novel-1", "山茶文具店", "小川糸 · 一间替人书写心意的小店。", SearchContentType.NOVEL, listOf("治愈", "日常")),
        SearchResult("dynamic-1", "雨天读完《山茶文具店》", "像收到一封温柔的信，让潮湿的下午也慢慢安静下来。", SearchContentType.DYNAMIC, listOf("读书笔记", "治愈系")),
        SearchResult("novel-2", "紫罗兰永恒花园", "晓佳奈 · 为理解爱而书写信件的故事。", SearchContentType.NOVEL, listOf("奇幻", "成长")),
        SearchResult("dynamic-2", "我的夏日阅读清单", "想把阅读的速度放慢一点，留些空白给散步和发呆。", SearchContentType.DYNAMIC, listOf("待读书单", "八月阅读")),
    )

    override suspend fun search(query: SearchQuery, page: Int, pageSize: Int): Result<SearchPage> = runCatching {
        val keyword = query.keyword.trim()
        require(keyword.isNotEmpty()) { "请输入搜索关键词" }
        val matched = allResults.filter { result ->
            val contentMatches = result.title.contains(keyword, true) || result.summary.contains(keyword, true)
            contentMatches && (query.filter.contentType == null || result.type == query.filter.contentType)
        }
        val start = page * pageSize
        val items = matched.drop(start).take(pageSize)
        SearchPage(items, nextPage = (page + 1).takeIf { start + items.size < matched.size })
    }

    override suspend fun getHistory(): Result<List<String>> = Result.success(history.toList())
    override suspend fun getHotKeywords(): Result<List<String>> = Result.success(listOf("山茶文具店", "夏日阅读", "治愈系", "赛博朋克"))

    override suspend fun saveHistory(keyword: String): Result<Unit> = runCatching {
        keyword.trim().takeIf { it.isNotEmpty() }?.let { normalized ->
            history.remove(normalized)
            history.add(0, normalized)
            if (history.size > MAX_HISTORY_SIZE) history.removeLast()
        }
    }

    override suspend fun clearHistory(): Result<Unit> = runCatching { history.clear() }

    private const val MAX_HISTORY_SIZE = 10
}
