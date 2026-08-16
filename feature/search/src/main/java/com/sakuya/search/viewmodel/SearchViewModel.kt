package com.sakuya.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.sakuya.model.search.SearchContentType
import com.sakuya.model.search.SearchFilter
import com.sakuya.model.search.SearchQuery
import com.sakuya.model.search.SearchResult
import com.sakuya.search.data.repository.SearchRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SearchViewModel.kt
 * 职责说明：管理入口页关键词、历史和发现词，以及结果页筛选和分页状态。
 * 执行流程：UI Action -> Repository 读取或写入数据 -> UiState 更新；失败消息单独以 Effect 发送给页面。
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
)

sealed interface SearchAction {
    data class UpdateKeyword(val keyword: String) : SearchAction
    data class LoadResults(val keyword: String) : SearchAction
    data class SelectType(val type: SearchContentType?) : SearchAction
    data object LoadMore : SearchAction
    data object ClearHistory : SearchAction
}

sealed interface SearchEffect { data class ShowError(val message: String) : SearchEffect }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()
    private val _effect = MutableSharedFlow<SearchEffect>()
    val effect = _effect.asSharedFlow()
    private var nextPage = 0

    init { loadDiscovery() }

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
            SearchAction.LoadMore -> if (_uiState.value.canLoadMore && !_uiState.value.isLoadingMore) {
                loadResults(_uiState.value.keyword, nextPage, refresh = false)
            }
            SearchAction.ClearHistory -> clearHistory()
        }
    }

    private fun loadDiscovery() = viewModelScope.launch {
        val history = repository.getHistory()
        val hotKeywords = repository.getHotKeywords()
        _uiState.value = _uiState.value.copy(
            history = history.getOrDefault(emptyList()),
            hotKeywords = hotKeywords.getOrDefault(emptyList()),
        )
        (history.exceptionOrNull() ?: hotKeywords.exceptionOrNull())?.let { error ->
            _effect.emit(SearchEffect.ShowError(error.message ?: "搜索数据加载失败"))
        }
    }

    /** 首次搜索覆盖旧页；加载更多只追加新页，分页指针完全由 Repository 返回结果决定。 */
    private fun loadResults(keyword: String, page: Int, refresh: Boolean) = viewModelScope.launch {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isEmpty()) return@launch
        _uiState.value = _uiState.value.copy(
            keyword = normalizedKeyword,
            isLoading = refresh,
            isLoadingMore = !refresh,
        )
        repository.search(
            SearchQuery(normalizedKeyword, SearchFilter(_uiState.value.selectedType)),
            page = page,
            pageSize = PAGE_SIZE,
        ).onSuccess { result ->
            if (refresh) repository.saveHistory(normalizedKeyword)
            nextPage = result.nextPage ?: page
            _uiState.value = _uiState.value.copy(
                results = if (refresh) result.items else _uiState.value.results + result.items,
                history = if (refresh) listOf(normalizedKeyword) + _uiState.value.history.filterNot { it == normalizedKeyword } else _uiState.value.history,
                isLoading = false,
                isLoadingMore = false,
                canLoadMore = result.nextPage != null,
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false)
            _effect.emit(SearchEffect.ShowError(error.message ?: "搜索失败"))
        }
    }

    private fun clearHistory() = viewModelScope.launch {
        repository.clearHistory().onSuccess { _uiState.value = _uiState.value.copy(history = emptyList()) }
            .onFailure { error -> _effect.emit(SearchEffect.ShowError(error.message ?: "清除历史失败")) }
    }

    private companion object { const val PAGE_SIZE = 20 }
}
