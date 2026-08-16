package com.sakuya.model.search

/**
 * SearchModels.kt
 * 职责说明：定义搜索输入、筛选、结果和分页等可跨 UI 复用的领域模型。
 * 执行流程：UI 组合 SearchQuery -> Repository 查询或缓存 -> ViewModel 将 SearchPage 写入 UiState。
 */
data class SearchQuery(
    val keyword: String,
    val filter: SearchFilter = SearchFilter(),
)

data class SearchFilter(
    val contentType: SearchContentType? = null,
)

enum class SearchContentType(val label: String) {
    DYNAMIC("动态"),
    NOVEL("轻小说"),
}

data class SearchResult(
    val id: String,
    val title: String,
    val summary: String,
    val type: SearchContentType,
    val tags: List<String> = emptyList(),
)

data class SearchPage(
    val items: List<SearchResult>,
    val nextPage: Int? = null,
)
