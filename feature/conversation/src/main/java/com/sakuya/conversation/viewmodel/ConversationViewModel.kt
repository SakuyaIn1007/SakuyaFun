package com.sakuya.conversation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.conversation.data.repository.ConversationRepository
import com.sakuya.conversation.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val repository: ConversationRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _effect = MutableSharedFlow<ConversationEffect>()
    val effect = _effect.asSharedFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getConversations()
                .onSuccess { list ->
                    _conversations.value = list
                }
                .onFailure { error ->
                    _effect.emit(ConversationEffect.ShowError(error.message ?: "加载会话列表失败"))
                }
            _isLoading.value = false
        }
    }

    fun onAction(action: ConversationAction) {
        when (action) {
            is ConversationAction.OnChatClick -> emitEffect(
                ConversationEffect.NavigateToChat(action.conversation)
            )
            is ConversationAction.OnNoticeClick -> emitEffect(ConversationEffect.NavigateToNotice)
            is ConversationAction.OnFriendClick -> emitEffect(ConversationEffect.NavigateToFriend)
            is ConversationAction.OnGroupClick -> emitEffect(ConversationEffect.NavigateToGroup)
        }
    }

    private fun emitEffect(effect: ConversationEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}

sealed interface ConversationAction {
    data class OnChatClick(val conversation: Conversation) : ConversationAction
    data object OnNoticeClick : ConversationAction
    data object OnFriendClick : ConversationAction
    data object OnGroupClick : ConversationAction
}

sealed interface ConversationEffect {
    data class NavigateToChat(val conversation: Conversation) : ConversationEffect
    data object NavigateToNotice : ConversationEffect
    data object NavigateToFriend : ConversationEffect
    data object NavigateToGroup : ConversationEffect
    data class ShowError(val message: String) : ConversationEffect
}
