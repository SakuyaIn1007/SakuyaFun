package com.sakuya.conversation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.sakuya.common.mvi.BaseMviViewModel
import com.sakuya.conversation.data.repository.ChatRepository
import com.sakuya.model.chat.ConversationDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatDetailUiState(
    val detail: ConversationDetail? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasLeftGroup: Boolean = false,
)

/** 聊天详情页所有用户意图的统一入口。 */
sealed interface ChatDetailAction {
    data object Load : ChatDetailAction
    data class UpdatePinned(val value: Boolean) : ChatDetailAction
    data class UpdateMuted(val value: Boolean) : ChatDetailAction
    data class UpdateMemberNickname(val value: String) : ChatDetailAction
    data object ClearLocalHistory : ChatDetailAction
    data object LeaveGroup : ChatDetailAction
}

/** 一次性提示与确认事件；持久化的页面状态（加载中、保存中、已退群）保留在 UiState。 */
sealed interface ChatDetailEffect {
    data class ShowError(val message: String) : ChatDetailEffect
    data object LocalHistoryCleared : ChatDetailEffect
}

/**
 * ChatDetailViewModel.kt
 * 职责说明：管理聊天详情资料、成员个人偏好和退出群聊操作。
 * 执行流程：进入页面加载详情；开关操作乐观展示并请求后端，失败时重新拉取真值；
 * 清空记录和退出群由 Repository 执行，UI 仅依赖不可变状态和 effect 通道。
 * 架构说明：MVI 风格，统一经 onAction 接收意图；错误与提示经 effect 一次性下发。
 */
@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChatRepository,
) : BaseMviViewModel<ChatDetailUiState, ChatDetailAction, ChatDetailEffect>(
    initialState = ChatDetailUiState(),
) {
    private val conversationId: String = savedStateHandle["conversationId"] ?: ""

    init { load() }

    override fun onAction(action: ChatDetailAction) {
        when (action) {
            ChatDetailAction.Load -> load()
            is ChatDetailAction.UpdatePinned -> updatePreferences(pinned = action.value)
            is ChatDetailAction.UpdateMuted -> updatePreferences(muted = action.value)
            is ChatDetailAction.UpdateMemberNickname -> updatePreferences(memberNickname = action.value)
            ChatDetailAction.ClearLocalHistory -> clearLocalHistory()
            ChatDetailAction.LeaveGroup -> leaveGroup()
        }
    }

    private fun load() = viewModelScope.launch {
        updateState { it.copy(isLoading = true) }
        repository.getConversationDetail(conversationId).onSuccess { detail ->
            updateState { it.copy(detail = detail, isLoading = false) }
        }.onFailure { error ->
            updateState { it.copy(isLoading = false) }
            sendEffect(ChatDetailEffect.ShowError(error.message ?: "加载聊天详情失败"))
        }
    }

    private fun updatePreferences(pinned: Boolean? = null, muted: Boolean? = null, memberNickname: String? = null) = viewModelScope.launch {
        updateState { it.copy(isSaving = true) }
        repository.updatePreferences(conversationId, pinned, muted, memberNickname).onSuccess { detail ->
            updateState { it.copy(detail = detail, isSaving = false) }
        }.onFailure { error ->
            updateState { it.copy(isSaving = false) }
            sendEffect(ChatDetailEffect.ShowError(error.message ?: "保存会话设置失败"))
            load()
        }
    }

    private fun clearLocalHistory() = viewModelScope.launch {
        repository.clearLocalMessages(conversationId)
        sendEffect(ChatDetailEffect.LocalHistoryCleared)
    }

    private fun leaveGroup() = viewModelScope.launch {
        updateState { it.copy(isSaving = true) }
        repository.leaveGroup(conversationId).onSuccess {
            updateState { it.copy(isSaving = false, hasLeftGroup = true) }
        }.onFailure { error ->
            updateState { it.copy(isSaving = false) }
            sendEffect(ChatDetailEffect.ShowError(error.message ?: "退出群聊失败"))
        }
    }
}
