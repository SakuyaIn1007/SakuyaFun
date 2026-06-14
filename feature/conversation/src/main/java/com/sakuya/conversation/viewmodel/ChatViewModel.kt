package com.sakuya.conversation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.conversation.data.remote.ChatMessageDto
import com.sakuya.conversation.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversationId: String = "",
    val title: String = "",
    val messages: List<ChatMessageDto> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isConnected: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val conversationId: String = savedStateHandle["conversationId"] ?: ""
    private val title: String = savedStateHandle["title"] ?: ""

    private val _uiState = MutableStateFlow(
        ChatUiState(
            conversationId = conversationId,
            title = title
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadHistoryMessages()
        observeRealtimeMessages()
        chatRepository.connect()
    }

    private fun loadHistoryMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            chatRepository.getHistoryMessages(conversationId)
                .onSuccess { messages ->
                    _uiState.update { it.copy(messages = messages, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun observeRealtimeMessages() {
        viewModelScope.launch {
            chatRepository.realtimeMessages.collect { message ->
                _uiState.update { state ->
                    val existingIds = state.messages.map { it.id }.toSet()
                    if (message.id !in existingIds) {
                        state.copy(messages = state.messages + message)
                    } else {
                        state
                    }
                }
            }
        }
    }

    //
    fun onInputChanged(value: String) {
        _uiState.update { it.copy(inputText = value) }
    }

    //
    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isEmpty()) return
        _uiState.update { it.copy(inputText = "") }
        chatRepository.sendMessage(conversationId, content)
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnect()
    }
}
