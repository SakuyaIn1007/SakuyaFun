package com.sakuya.catalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.catalog.data.repository.CatalogRepository
import com.sakuya.catalog.model.ContentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadCatalogData()
    }

    /**
     * 推荐与周榜独立加载，任何一路失败都不能清空另一条已经成功返回的 Wenku8 缓存数据。
     * 执行流程：并行请求两个后端接口 -> 分别写入对应列表/错误字段 -> 两路结束后关闭全页加载态。
     */
    fun loadCatalogData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, recommendationError = null, novelError = null) }
            coroutineScope {
                val recommendations = async { catalogRepository.getRecommendItems() }
                val novels = async { catalogRepository.getNovelItems() }
                recommendations.await().onSuccess { items -> _uiState.update { it.copy(recommendItems = items, recommendationError = null) } }
                    .onFailure { error -> _uiState.update { it.copy(recommendationError = error.message ?: "加载推荐失败") } }
                novels.await().onSuccess { items -> _uiState.update { it.copy(novelItems = items, novelError = null) } }
                    .onFailure { error -> _uiState.update { it.copy(novelError = error.message ?: "加载轻小说失败") } }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun onAction(action: CatalogAction) {
        when (action) {
            is CatalogAction.OnSearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = action.query) }
            }
            is CatalogAction.OnSearchClear -> {
                _uiState.update { it.copy(searchQuery = "") }
            }
        }
    }
}

data class CatalogUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val recommendItems: List<ContentItem> = emptyList(),
    val novelItems: List<ContentItem> = emptyList(),
    val selectedTab: Int = 0,
    val recommendationError: String? = null,
    val novelError: String? = null,
)

sealed interface CatalogAction {
    data class OnSearchQueryChanged(val query: String) : CatalogAction
    data object OnSearchClear : CatalogAction
}
