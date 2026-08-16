package com.sakuya.friend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.friend.data.repository.GroupRepository
import com.sakuya.model.group.GroupConversation
import com.sakuya.model.group.GroupJoinRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * GroupViewModel.kt
 * 职责说明：管理群会话列表和入群申请操作，不让 GroupScreen 维护任何示例数据或网络状态。
 * 执行流程：UI Action -> GroupRepository -> GroupUiState；申请审核的成功或错误通过 Effect 告知页面。
 */
data class GroupUiState(
    val conversations: List<GroupConversation> = emptyList(),
    val isLoading: Boolean = true,
    val operatingRequestId: String? = null,
)

sealed interface GroupAction {
    data object Refresh : GroupAction
    data class RequestToJoin(val groupId: String, val message: String) : GroupAction
    data class ApproveJoinRequest(val groupId: String, val requestId: String) : GroupAction
    data class RejectJoinRequest(val groupId: String, val requestId: String) : GroupAction
}

sealed interface GroupEffect {
    data class ShowError(val message: String) : GroupEffect
    data class ShowMessage(val message: String) : GroupEffect
}

@HiltViewModel
class GroupViewModel @Inject constructor(private val repository: GroupRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState = _uiState.asStateFlow()
    private val _effect = MutableSharedFlow<GroupEffect>()
    val effect = _effect.asSharedFlow()

    init { refresh() }

    fun onAction(action: GroupAction) {
        when (action) {
            GroupAction.Refresh -> refresh()
            is GroupAction.RequestToJoin -> requestToJoin(action.groupId, action.message)
            is GroupAction.ApproveJoinRequest -> reviewRequest(action.groupId, action.requestId, approved = true)
            is GroupAction.RejectJoinRequest -> reviewRequest(action.groupId, action.requestId, approved = false)
        }
    }

    private fun refresh() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        repository.getGroupConversations().onSuccess { conversations ->
            _uiState.value = _uiState.value.copy(conversations = conversations, isLoading = false)
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(isLoading = false)
            _effect.emit(GroupEffect.ShowError(error.message ?: "加载群聊失败"))
        }
    }

    private fun requestToJoin(groupId: String, message: String) = viewModelScope.launch {
        repository.requestToJoin(groupId, message).onSuccess {
            _effect.emit(GroupEffect.ShowMessage("入群申请已提交"))
        }.onFailure { error -> _effect.emit(GroupEffect.ShowError(error.message ?: "提交入群申请失败")) }
    }

    /** 申请审核以 requestId 加锁，防止管理员连续点击导致重复提交。 */
    private fun reviewRequest(groupId: String, requestId: String, approved: Boolean) = viewModelScope.launch {
        if (_uiState.value.operatingRequestId != null) return@launch
        _uiState.value = _uiState.value.copy(operatingRequestId = requestId)
        val request: Result<GroupJoinRequest> = if (approved) {
            repository.approveJoinRequest(groupId, requestId)
        } else {
            repository.rejectJoinRequest(groupId, requestId)
        }
        request.onSuccess {
            _effect.emit(GroupEffect.ShowMessage(if (approved) "已通过入群申请" else "已拒绝入群申请"))
        }.onFailure { error -> _effect.emit(GroupEffect.ShowError(error.message ?: "审核入群申请失败")) }
        _uiState.value = _uiState.value.copy(operatingRequestId = null)
    }
}
