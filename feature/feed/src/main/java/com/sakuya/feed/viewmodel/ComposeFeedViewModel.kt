package com.sakuya.feed.viewmodel

import androidx.lifecycle.viewModelScope
import com.sakuya.common.mvi.BaseMviViewModel
import com.sakuya.feed.data.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.sakuya.model.feed.FeedDraft
import kotlinx.coroutines.launch

/**
 * ComposeFeedViewModel.kt
 * 职责说明：管理动态发布表单和发布副作用。
 * 执行流程：输入 Action 更新表单 -> Publish 构造草稿并提交仓库 -> 发布结果通过 State/Effect 返回页面。
 * 架构说明：MVI 风格，基于 BaseMviViewModel 收敛状态与一次性事件样板。
 */
data class ComposeFeedUiState(val title: String = "", val content: String = "", val topicInput: String = "", val isPublishing: Boolean = false)
sealed interface ComposeFeedAction { data class UpdateTitle(val title: String) : ComposeFeedAction; data class UpdateContent(val content: String) : ComposeFeedAction; data class UpdateTopic(val topic: String) : ComposeFeedAction; data object Publish : ComposeFeedAction }
sealed interface ComposeFeedEffect { data object Published : ComposeFeedEffect; data class ShowError(val message: String) : ComposeFeedEffect }
@HiltViewModel class ComposeFeedViewModel @Inject constructor(private val repository: FeedRepository) : BaseMviViewModel<ComposeFeedUiState, ComposeFeedAction, ComposeFeedEffect>(
    initialState = ComposeFeedUiState(),
) {
    override fun onAction(action: ComposeFeedAction) {
        when (action) {
            is ComposeFeedAction.UpdateTitle -> updateState { it.copy(title = action.title) }
            is ComposeFeedAction.UpdateContent -> updateState { it.copy(content = action.content) }
            is ComposeFeedAction.UpdateTopic -> updateState { it.copy(topicInput = action.topic) }
            ComposeFeedAction.Publish -> publish()
        }
    }

    private fun publish() = viewModelScope.launch {
        val state = currentState
        updateState { it.copy(isPublishing = true) }
        repository.publish(FeedDraft(state.title, state.content, state.topicInput.split(" ").filter { it.isNotBlank() }.map { if (it.startsWith("#")) it else "#$it" })).onSuccess {
            updateState { it.copy(isPublishing = false) }
            sendEffect(ComposeFeedEffect.Published)
        }.onFailure { error ->
            updateState { it.copy(isPublishing = false) }
            sendEffect(ComposeFeedEffect.ShowError(error.message ?: "发布失败"))
        }
    }
}
