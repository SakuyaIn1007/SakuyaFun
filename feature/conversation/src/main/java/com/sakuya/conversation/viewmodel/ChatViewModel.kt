package com.sakuya.conversation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.conversation.data.remote.ConnectionState
import com.sakuya.conversation.data.repository.ChatRepository
import com.sakuya.conversation.model.ChatMessage
import com.sakuya.conversation.model.ChatMessageDraft
import com.sakuya.conversation.model.ChatSendStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

data class ChatUiState(
    val conversationId: String = "",
    val title: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val showWebSocketConnectionError: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
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
        /**
         * 先创建 Room 的会话父记录，再启动所有聊天数据流。
         * 这保证从好友页直接进入聊天时，历史消息不会因外键缺少父记录而写入失败。
         */
        viewModelScope.launch {
            chatRepository.ensureConversation(conversationId, title)
            observeLocalMessages()
            loadHistoryMessages()
            observeRealtimeMessages()
            observeConnectionState()
            chatRepository.connect()
        }
    }

    private fun loadHistoryMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            chatRepository.syncHistoryMessages(conversationId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 先订阅 Room，再执行网络同步，保证断网或 WebSocket 不可用时仍优先显示本地历史消息。
     * 消息写入均由 Repository 完成，UI 不再维护与数据库分离的第二份列表。
     */
    private fun observeLocalMessages() {
        viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun observeRealtimeMessages() {
        viewModelScope.launch {
            chatRepository.realtimeMessages.collect { message ->
                if (message.conversationId != conversationId) return@collect
                chatRepository.saveRealtimeMessage(message)
                chatRepository.markAsRead(conversationId)
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            chatRepository.connectionState.collect { state ->
                _uiState.update {
                    /**
                     * WebSocket 失败后会立刻进入重连状态，因此错误提示需要保留到真正连通时才隐藏。
                     * 这样页面能稳定显示一次连接失败信息，而不影响历史消息列表。
                     */
                    it.copy(
                        isConnected = state == ConnectionState.CONNECTED,
                        showWebSocketConnectionError = when (state) {
                            ConnectionState.CONNECTED -> false
                            ConnectionState.FAILED -> true
                            else -> it.showWebSocketConnectionError
                        }
                    )
                }
            }
        }
    }

    /** 输入变化只更新表单状态，发送逻辑集中在 sendMessage 以便统一维护乐观消息状态。 */
    fun onInputChanged(value: String) {
        _uiState.update { it.copy(inputText = value, errorMessage = null) }
    }

    /**
     * 先插入 PENDING 消息，再用服务端返回值替换；失败时保留消息并标记 FAILED，避免用户输入丢失。
     */
    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isEmpty()) return
        viewModelScope.launch {
            val pendingMessage = ChatMessage(
                id = "local-${UUID.randomUUID()}",
                conversationId = conversationId,
                content = content,
                timeLabel = "刚刚",
                isMine = true,
                avatarText = "我",
                timestamp = System.currentTimeMillis(),
                sendStatus = ChatSendStatus.PENDING,
            )
            _uiState.update { it.copy(inputText = "", isSending = true, errorMessage = null) }
            chatRepository.saveLocalMessage(pendingMessage)
            chatRepository.sendMessage(conversationId, ChatMessageDraft(content = content))
                .onSuccess { message ->
                    chatRepository.removeLocalMessage(pendingMessage.id)
                    _uiState.update { it.copy(isSending = false) }
                }
                .onFailure { error ->
                    chatRepository.saveLocalMessage(pendingMessage.copy(sendStatus = ChatSendStatus.FAILED))
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = error.message ?: "消息发送失败"
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnect()
    }
}
