package com.sakuya.catalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.catalog.data.repository.RankingRepository
import com.sakuya.catalog.model.RankingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val rankingRepository: RankingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRanking()
    }

    fun loadRanking() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            rankingRepository.getRankingItems()
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(isLoading = false, items = items, error = null)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = emptyList(),
                            error = error.message ?: "加载排行榜失败"
                        )
                    }
                }
        }
    }

    fun retry() {
        loadRanking()
    }
}

data class RankingUiState(
    val isLoading: Boolean = true,
    val items: List<RankingItem> = emptyList(),
    val error: String? = null
)
