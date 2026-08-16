package com.sakuya.conversation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ChatViewModel.kt
 * 职责说明：
 * 1. 统一管理聊天页的输入、Room 消息列表与发送状态。
 * 2. 先订阅 Room 历史记录，再在后台同步 REST 历史和接收 WebSocket 实时消息。
 * 3. 将发送中的本地消息持久化；发送失败后保留失败状态并提供重试入口。
 * 执行流程：页面进入后确保会话存在并立即观察 Room；网络数据只写入 Room，
 * WebSocket 连接失败由底层静默重连，不改变页面的离线历史展示。
 */
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
            sendPendingMessage(pendingMessage)
        }
    }

    /**
     * 重试只针对 Room 中已失败的本地消息，保留原有内容、附件和回复关系。
     * 执行流程：先将状态恢复为 PENDING，再复用发送流程；成功删除临时记录，失败重新标记 FAILED。
     */
    fun retryMessage(message: ChatMessage) {
        if (message.sendStatus != ChatSendStatus.FAILED) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            sendPendingMessage(message.copy(sendStatus = ChatSendStatus.PENDING))
        }
    }

    /** 将乐观消息写入 Room 后发送；无论页面是否重建，发送结果都以本地消息状态为准。 */
    private suspend fun sendPendingMessage(pendingMessage: ChatMessage) {
        chatRepository.saveLocalMessage(pendingMessage)
        chatRepository.sendMessage(
            conversationId = conversationId,
            draft = ChatMessageDraft(
                content = pendingMessage.content,
                attachments = pendingMessage.attachments,
                replyTo = pendingMessage.replyTo,
            )
        ).onSuccess {
            chatRepository.removeLocalMessage(pendingMessage.id)
            _uiState.update { it.copy(isSending = false) }
        }.onFailure { error ->
            chatRepository.saveLocalMessage(pendingMessage.copy(sendStatus = ChatSendStatus.FAILED))
            _uiState.update {
                it.copy(isSending = false, errorMessage = error.message ?: "消息发送失败")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnect()
    }
}
