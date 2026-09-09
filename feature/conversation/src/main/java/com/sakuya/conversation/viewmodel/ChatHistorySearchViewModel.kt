package com.sakuya.conversation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.sakuya.common.mvi.BaseMviViewModel
import com.sakuya.conversation.data.repository.ChatRepository
import com.sakuya.model.chat.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatHistorySearchUiState(val query: String = "", val results: List<ChatMessage> = emptyList(), val isLoading: Boolean = false)

/** 聊天记录搜索页的用户意图；输入更新与提交搜索统一经 onAction 进入。 */
sealed interface ChatHistorySearchAction {
    data class QueryChanged(val value: String) : ChatHistorySearchAction
    data object Search : ChatHistorySearchAction
}

/** 搜索失败与空关键词校验等一次性提示。 */
sealed interface ChatHistorySearchEffect {
    data class ShowError(val message: String) : ChatHistorySearchEffect
}

/**
 * ChatHistorySearchViewModel.kt
 * 职责说明：管理当前会话的服务端全文检索和结果状态。
 * 执行流程：输入只更新表单；提交非空关键词后查询当前会话，点击结果由导航层携带消息 ID 返回聊天页定位。
 * 架构说明：MVI 风格，状态与一次性提示分离。
 */
@HiltViewModel
class ChatHistorySearchViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val repository: ChatRepository) : BaseMviViewModel<ChatHistorySearchUiState, ChatHistorySearchAction, ChatHistorySearchEffect>(
    initialState = ChatHistorySearchUiState(),
) {
    private val conversationId: String = savedStateHandle["conversationId"] ?: ""

    override fun onAction(action: ChatHistorySearchAction) {
        when (action) {
            is ChatHistorySearchAction.QueryChanged -> updateState { it.copy(query = action.value) }
            ChatHistorySearchAction.Search -> search()
        }
    }

    private fun search() {
        val query = currentState.query.trim()
        if (query.isEmpty()) {
            updateState { it.copy(results = emptyList()) }
            sendEffect(ChatHistorySearchEffect.ShowError("请输入搜索关键词"))
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            repository.searchMessages(conversationId, query).onSuccess { results ->
                updateState { it.copy(results = results, isLoading = false) }
            }.onFailure { error ->
                updateState { it.copy(isLoading = false) }
                sendEffect(ChatHistorySearchEffect.ShowError(error.message ?: "搜索聊天记录失败"))
            }
        }
    }
}
