package com.sakuya.catalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.catalog.data.repository.ScheduleRepository
import com.sakuya.catalog.model.NovelRelease
import com.sakuya.catalog.model.NovelReleaseDayGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ScheduleViewModel.kt
 * 职责说明：整理时间表页的推荐与按日期分组的更新列表，页面只订阅一个 UiState。
 * 执行流程：Repository 返回所有已公布更新 -> 过滤有效日期并分组排序 -> 输出 Banner 推荐和日期条目。
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    /** 拉取全部已公布更新；网络失败时 Repository 已回退本地资料，页面仍保留可用时间表。 */
    fun loadSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            scheduleRepository.getPublishedReleases()
                .onSuccess { releases ->
                    _uiState.value = ScheduleUiState(
                        isLoading = false,
                        recommendedReleases = releases.filter(NovelRelease::isRecommended),
                        releaseGroups = releases
                            .groupBy(NovelRelease::releaseDate)
                            .toSortedMap()
                            .map { (date, dayReleases) -> NovelReleaseDayGroup(date, dayReleases) },
                    )
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "时间表加载失败") }
                }
        }
    }
}

data class ScheduleUiState(
    val recommendedReleases: List<NovelRelease> = emptyList(),
    val releaseGroups: List<NovelReleaseDayGroup> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
