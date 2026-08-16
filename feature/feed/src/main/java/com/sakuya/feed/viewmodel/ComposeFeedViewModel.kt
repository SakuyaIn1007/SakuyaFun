package com.sakuya.feed.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.feed.data.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.sakuya.model.feed.FeedDraft
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ComposeFeedViewModel.kt
 * 职责说明：管理动态发布表单和发布副作用。
 * 执行流程：输入 Action 更新表单 -> Publish 构造草稿并提交仓库 -> 发布结果通过 State/Effect 返回页面。
 */
data class ComposeFeedUiState(val title: String = "", val content: String = "", val topicInput: String = "", val isPublishing: Boolean = false)
sealed interface ComposeFeedAction { data class UpdateTitle(val title: String) : ComposeFeedAction; data class UpdateContent(val content: String) : ComposeFeedAction; data class UpdateTopic(val topic: String) : ComposeFeedAction; data object Publish : ComposeFeedAction }
sealed interface ComposeFeedEffect { data object Published : ComposeFeedEffect; data class ShowError(val message: String) : ComposeFeedEffect }
@HiltViewModel class ComposeFeedViewModel @Inject constructor(private val repository: FeedRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ComposeFeedUiState())
    val uiState = _uiState.asStateFlow()
    private val _effect = MutableSharedFlow<ComposeFeedEffect>()
    val effect = _effect.asSharedFlow()
    fun onAction(action: ComposeFeedAction) = when (action) { is ComposeFeedAction.UpdateTitle -> _uiState.value = _uiState.value.copy(title = action.title); is ComposeFeedAction.UpdateContent -> _uiState.value = _uiState.value.copy(content = action.content); is ComposeFeedAction.UpdateTopic -> _uiState.value = _uiState.value.copy(topicInput = action.topic); ComposeFeedAction.Publish -> publish() }
    private fun publish() = viewModelScope.launch {
        val state = _uiState.value
        _uiState.value = state.copy(isPublishing = true)
        repository.publish(FeedDraft(state.title, state.content, state.topicInput.split(" ").filter { it.isNotBlank() }.map { if (it.startsWith("#")) it else "#$it" })).onSuccess {
            _uiState.value = _uiState.value.copy(isPublishing = false); _effect.emit(ComposeFeedEffect.Published)
        }.onFailure { error -> _uiState.value = _uiState.value.copy(isPublishing = false); _effect.emit(ComposeFeedEffect.ShowError(error.message ?: "发布失败")) }
    }
}
