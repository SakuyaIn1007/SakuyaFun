package com.sakuya.catalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.catalog.data.repository.CatalogRepository
import com.sakuya.catalog.model.ContentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    fun loadCatalogData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            catalogRepository.getRecommendItems()
                .onSuccess { items ->
                    _uiState.update { it.copy(recommendItems = items) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "加载推荐失败")
                    }
                }

            catalogRepository.getNovelItems()
                .onSuccess { items ->
                    _uiState.update { it.copy(novelItems = items) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "加载轻小说失败")
                    }
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
    val error: String? = null
)

sealed interface CatalogAction {
    data class OnSearchQueryChanged(val query: String) : CatalogAction
    data object OnSearchClear : CatalogAction
}
