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
 * 执行流程：导航参数提供小说 ID -> 进页只读元数据 -> 用户选择章节阅读时才加载目录。
 *
 * 目录必须懒加载：后端在目录缺失时会回退到下载源抓取整本 TXT（数 MB）。
 * 若进页即拉目录，每打开一个详情页都会触发一次完整下载，与「详情页只读元数据」的设计冲突。
 */
data class Wenku8DetailUiState(
    val novel: ContentNovelDto? = null,
    val index: ContentChapterIndexDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
    /** 用户是否已选择「章节阅读」。为 true 时才展示目录。 */
    val chapterMode: Boolean = false,
    /** 目录单独加载中：与整页 loading 区分，避免展开目录时整页闪烁。 */
    val indexLoading: Boolean = false,
    val indexError: String? = null,
)

@HiltViewModel
class Wenku8DetailViewModel @Inject constructor(
    private val repository: ContentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val novelId: String = savedStateHandle[WENKU8_DETAIL_ARG_ID] ?: ""
    private val _uiState = MutableStateFlow(Wenku8DetailUiState())
    val uiState = _uiState.asStateFlow()

    init { loadMetadata() }

    /** 进页只读元数据。书目信息此前已由目录同步写入本地库，不会触发内容抓取。 */
    fun loadMetadata() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        val novel = repository.novel(novelId)
        _uiState.value = _uiState.value.copy(
            novel = novel.getOrNull(),
            loading = false,
            error = novel.exceptionOrNull()?.message
        )
    }

    /** 用户选择「章节阅读」时调用。首次进入才请求目录，避免未选择就触发下载。 */
    fun selectChapterMode() {
        _uiState.value = _uiState.value.copy(chapterMode = true)
        if (_uiState.value.index != null || _uiState.value.indexLoading) return
        loadIndex()
    }

    /** 收起目录不丢弃已加载数据：再次展开无需重新下载。 */
    fun collapseChapterMode() {
        _uiState.value = _uiState.value.copy(chapterMode = false)
    }

    fun loadIndex() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(indexLoading = true, indexError = null)
        val index = repository.chapters(novelId)
        _uiState.value = _uiState.value.copy(
            index = index.getOrNull(),
            indexLoading = false,
            indexError = index.exceptionOrNull()?.message
        )
    }
}
