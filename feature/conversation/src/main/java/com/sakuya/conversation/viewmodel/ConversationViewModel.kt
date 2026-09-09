package com.sakuya.conversation.viewmodel

import androidx.lifecycle.viewModelScope
import com.sakuya.common.mvi.BaseMviViewModel
import com.sakuya.conversation.data.repository.ConversationRepository
import com.sakuya.data.notification.NotificationRepository
import com.sakuya.data.notification.UpdateBadgeRepository
import com.sakuya.model.chat.Conversation
import com.sakuya.model.notification.UpdateBadgeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 会话列表页的单一不可变状态；红点与好友更新提示统一收敛，避免多条流各自驱动页面。 */
data class ConversationUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
    val notificationUnreadCount: Int = 0,
    val updateBadgeState: UpdateBadgeState = UpdateBadgeState(),
)

sealed interface ConversationAction {
    data class OnChatClick(val conversation: Conversation) : ConversationAction
    data object OnNoticeClick : ConversationAction
    data object OnFriendClick : ConversationAction
    data object OnGroupClick : ConversationAction
    data object Refresh : ConversationAction
}

sealed interface ConversationEffect {
    data class NavigateToChat(val conversation: Conversation) : ConversationEffect
    data object NavigateToNotice : ConversationEffect
    data object NavigateToFriend : ConversationEffect
    data object NavigateToGroup : ConversationEffect
    data class ShowError(val message: String) : ConversationEffect
}

/**
 * ConversationViewModel.kt
 * 职责说明：管理会话列表、未读通知红点与好友更新提示状态。
 * 执行流程：UI 通过 onAction 提交意图 -> 单一 UiState 更新 -> 导航与错误经 effect 一次性下发。
 * 架构说明：MVI 风格；Room 先输出缓存会话，网络刷新完成后的数据经同一条状态流自动更新页面。
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val repository: ConversationRepository,
    private val notificationRepository: NotificationRepository,
    updateBadges: UpdateBadgeRepository,
) : BaseMviViewModel<ConversationUiState, ConversationAction, ConversationEffect>(
    initialState = ConversationUiState(),
) {

    init {
        observeLocalConversations()
        onAction(ConversationAction.Refresh)
        observeNotificationBadges(updateBadges)
        viewModelScope.launch { notificationRepository.refresh() }
    }

    override fun onAction(action: ConversationAction) {
        when (action) {
            is ConversationAction.OnChatClick -> sendEffect(ConversationEffect.NavigateToChat(action.conversation))
            ConversationAction.OnNoticeClick -> sendEffect(ConversationEffect.NavigateToNotice)
            ConversationAction.OnFriendClick -> sendEffect(ConversationEffect.NavigateToFriend)
            ConversationAction.OnGroupClick -> sendEffect(ConversationEffect.NavigateToGroup)
            ConversationAction.Refresh -> loadConversations()
        }
    }

    /** Room 先输出缓存会话；网络刷新完成后的数据会通过同一条状态流自动更新页面。 */
    private fun observeLocalConversations() {
        viewModelScope.launch {
            repository.observeConversations().collect { conversations ->
                updateState { it.copy(conversations = conversations) }
            }
        }
    }

    /** 通知红点与好友更新提示是共享的全局状态，收敛进会话页的单一 UiState。 */
    private fun observeNotificationBadges(updateBadges: UpdateBadgeRepository) {
        viewModelScope.launch {
            notificationRepository.unreadCount.collect { count ->
                updateState { it.copy(notificationUnreadCount = count) }
            }
        }
        viewModelScope.launch {
            updateBadges.state.collect { badge ->
                updateState { it.copy(updateBadgeState = badge) }
            }
        }
    }

    private fun loadConversations() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            repository.refreshConversations()
                .onSuccess { }
                .onFailure { error ->
                    sendEffect(ConversationEffect.ShowError(error.message ?: "加载会话列表失败"))
                }
            updateState { it.copy(isLoading = false) }
        }
    }
}
