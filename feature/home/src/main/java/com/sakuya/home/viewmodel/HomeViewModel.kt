package com.sakuya.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.home.data.repository.HomeRepository
import com.sakuya.home.model.ContentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect = _effect.asSharedFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            homeRepository.getRecommendItems()
                .onSuccess { items ->
                    _uiState.update { it.copy(recommendItems = items) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "加载推荐失败")
                    }
                }

            homeRepository.getNovelItems()
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

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnSearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = action.query) }
            }
            HomeAction.OnSearchClear -> {
                _uiState.update { it.copy(searchQuery = "") }
            }
            HomeAction.OnRankingClick -> emitEffect(HomeEffect.NavigateToRanking)
        }
    }

    private fun emitEffect(effect: HomeEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val recommendItems: List<ContentItem> = emptyList(),
    val novelItems: List<ContentItem> = emptyList(),
    val selectedTab: Int = 0,
    val error: String? = null
)

sealed interface HomeEffect {
    data object NavigateToRanking : HomeEffect
}

sealed interface HomeAction {
    data class OnSearchQueryChanged(val query: String) : HomeAction
    data object OnSearchClear : HomeAction
    data object OnRankingClick : HomeAction
}
