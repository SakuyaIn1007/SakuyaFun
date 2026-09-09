package com.sakuya.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.sakuya.model.search.SearchContentType
import com.sakuya.model.search.SearchFilter
import com.sakuya.model.search.SearchQuery
import com.sakuya.model.search.SearchResult
import com.sakuya.search.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SearchViewModel.kt
 * 职责说明：管理入口页关键词、历史和发现词，以及结果页筛选和分页状态。
 * 执行流程：UI Action -> Repository 读取或写入数据 -> UiState 更新；搜索与发现加载失败保留在状态中供页面重试。
 */
data class SearchUiState(
    val keyword: String = "",
    val history: List<String> = emptyList(),
    val hotKeywords: List<String> = emptyList(),
    val selectedType: SearchContentType? = null,
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val errorMessage: String? = null,
    val discoveryErrorMessage: String? = null,
)

sealed interface SearchAction {
    data class UpdateKeyword(val keyword: String) : SearchAction
    data class LoadResults(val keyword: String) : SearchAction
    data class SelectType(val type: SearchContentType?) : SearchAction
    data object LoadDiscovery : SearchAction
    data object LoadMore : SearchAction
    data object Retry : SearchAction
    data object ClearHistory : SearchAction
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()
    private var nextPage = 0
    private var searchJob: Job? = null

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.UpdateKeyword -> _uiState.value = _uiState.value.copy(keyword = action.keyword)
            is SearchAction.LoadResults -> loadResults(action.keyword, page = 0, refresh = true)
            is SearchAction.SelectType -> {
                if (_uiState.value.selectedType != action.type) {
                    _uiState.value = _uiState.value.copy(selectedType = action.type)
                    _uiState.value.keyword.takeIf { it.isNotBlank() }?.let { loadResults(it, page = 0, refresh = true) }
                }
            }
            SearchAction.LoadDiscovery -> loadDiscovery()
            SearchAction.LoadMore -> if (_uiState.value.canLoadMore && !_uiState.value.isLoadingMore) {
                loadResults(_uiState.value.keyword, nextPage, refresh = false)
            }
            SearchAction.Retry -> {
                if (_uiState.value.results.isEmpty()) loadResults(_uiState.value.keyword, page = 0, refresh = true)
                else loadResults(_uiState.value.keyword, page = nextPage, refresh = false)
            }
            SearchAction.ClearHistory -> clearHistory()
        }
    }

    private fun loadDiscovery() = viewModelScope.launch {
        val history = repository.getHistory()
        val hotKeywords = repository.getHotKeywords()
        val discoveryError = history.exceptionOrNull() ?: hotKeywords.exceptionOrNull()
        _uiState.value = _uiState.value.copy(
            history = history.getOrDefault(emptyList()),
            hotKeywords = hotKeywords.getOrDefault(emptyList()),
            discoveryErrorMessage = discoveryError?.message ?: discoveryError?.let { "搜索数据加载失败" },
        )
    }

    /** 首次搜索会取消旧条件请求；加载更多拒绝重复触发，避免慢响应覆盖新筛选或重复追加同一页。 */
    private fun loadResults(keyword: String, page: Int, refresh: Boolean) {
        if (refresh) searchJob?.cancel() else if (searchJob?.isActive == true) return
        searchJob = viewModelScope.launch {
            val normalizedKeyword = keyword.trim()
            if (normalizedKeyword.isEmpty()) return@launch
            _uiState.value = _uiState.value.copy(
                keyword = normalizedKeyword,
                results = if (refresh) emptyList() else _uiState.value.results,
                isLoading = refresh,
                isLoadingMore = !refresh,
                canLoadMore = if (refresh) false else _uiState.value.canLoadMore,
                errorMessage = null,
            )
            repository.search(
                SearchQuery(normalizedKeyword, SearchFilter(_uiState.value.selectedType)),
                page = page,
                pageSize = PAGE_SIZE,
            ).onSuccess { result ->
                if (refresh) {
                    repository.saveHistory(normalizedKeyword).onFailure { error ->
                        // 搜索结果仍可正常使用；保存失败留到入口页提示，避免把非关键错误伪装成搜索失败。
                        _uiState.value = _uiState.value.copy(discoveryErrorMessage = error.message ?: "搜索历史保存失败")
                    }
                }
                nextPage = result.nextPage ?: 0
                _uiState.value = _uiState.value.copy(
                    results = if (refresh) result.items else (_uiState.value.results + result.items).distinctBy { it.type to it.id },
                    history = if (refresh) listOf(normalizedKeyword) + _uiState.value.history.filterNot { it.equals(normalizedKeyword, ignoreCase = true) } else _uiState.value.history,
                    isLoading = false,
                    isLoadingMore = false,
                    canLoadMore = result.nextPage != null,
                    errorMessage = null,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = error.message ?: "搜索失败",
                )
            }
        }
    }

    private fun clearHistory() = viewModelScope.launch {
        repository.clearHistory().onSuccess {
            _uiState.value = _uiState.value.copy(history = emptyList())
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(discoveryErrorMessage = error.message ?: "清除历史失败")
        }
    }

    private companion object { const val PAGE_SIZE = 20 }
}
