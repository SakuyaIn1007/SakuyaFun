package com.sakuya.search.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.navigation.WENKU8_DETAIL_ARG_ID
import com.sakuya.data.content.ContentChapterIndexDto
import com.sakuya.data.content.ContentNovelDto
import com.sakuya.search.data.content.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Wenku8DetailViewModel.kt
 * 职责说明：协调远端小说详情与目录加载，并让详情页只渲染不可变 UiState。
 * 执行流程：导航参数提供小说 ID -> 并行读取详情和目录 -> 成功写入 UiState，失败保留重试入口。
 */
data class Wenku8DetailUiState(val novel: ContentNovelDto? = null, val index: ContentChapterIndexDto? = null, val loading: Boolean = true, val error: String? = null)

@HiltViewModel
class Wenku8DetailViewModel @Inject constructor(
    private val repository: ContentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val novelId: String = savedStateHandle[WENKU8_DETAIL_ARG_ID] ?: ""
    private val _uiState = MutableStateFlow(Wenku8DetailUiState())
    val uiState = _uiState.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        val novel = repository.novel(novelId)
        val index = repository.chapters(novelId)
        val error = novel.exceptionOrNull() ?: index.exceptionOrNull()
        _uiState.value = Wenku8DetailUiState(novel.getOrNull(), index.getOrNull(), false, error?.message)
    }
}
