package com.sakuya.conversation.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.sakuya.common.mvi.BaseMviViewModel
import com.sakuya.conversation.data.repository.ChatRepository
import com.sakuya.model.chat.ChatAttachment
import com.sakuya.model.chat.ChatMessage
import com.sakuya.model.chat.ChatMessageDraft
import com.sakuya.model.chat.ChatMessageReply
import com.sakuya.model.chat.ChatMessageType
import com.sakuya.model.chat.ChatSendStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

data class ChatUiState(
    val conversationId: String = "",
    val title: String = "",
    val focusMessageId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val draftAttachments: List<ChatAttachment> = emptyList(),
    val replyingTo: ChatMessageReply? = null,
    val isUploadingAttachment: Boolean = false,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val historyErrorMessage: String? = null,
)

/** 聊天页所有用户意图的统一入口；一次性错误通过 [ChatEffect] 回传页面。 */
sealed interface ChatAction {
    data class InputChanged(val value: String) : ChatAction
    data class AttachmentSelected(val uri: Uri) : ChatAction
    data class RemoveDraftAttachment(val attachmentId: String) : ChatAction
    data class ReplyTo(val message: ChatMessage) : ChatAction
    data object CancelReply : ChatAction
    data object SendMessage : ChatAction
    data class RetryMessage(val message: ChatMessage) : ChatAction
    data object RetryHistory : ChatAction
}

/** 一次性提示（附件上传失败、发送失败等）；历史加载失败因需要持久重试入口，保留在 UiState。 */
sealed interface ChatEffect {
    data class ShowError(val message: String) : ChatEffect
}

/**
 * ChatViewModel.kt
 * 职责说明：
 * 1. 统一管理聊天页的输入、Room 消息列表与发送状态。
 * 2. 先订阅 Room 历史记录，再在后台同步 REST 历史和接收 WebSocket 实时消息。
 * 3. 将发送中的本地消息持久化；发送失败后保留失败状态并提供重试入口。
 * 执行流程：页面通过 onAction 提交意图 -> 更新单一不可变 UiState -> 一次性错误经 effect 下发。
 * 架构说明：MVI 风格，状态只通过 updateState 修改；网络失败不清空 Room 缓存展示。
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository
) : BaseMviViewModel<ChatUiState, ChatAction, ChatEffect>(
    initialState = ChatUiState(
        conversationId = savedStateHandle["conversationId"] ?: "",
        title = savedStateHandle["title"] ?: "",
        focusMessageId = savedStateHandle["focusMessageId"] ?: "",
        isLoading = (savedStateHandle["conversationId"] as? String).orEmpty().isNotBlank(),
    )
) {

    private val conversationId: String = savedStateHandle["conversationId"] ?: ""
    private val title: String = savedStateHandle["title"] ?: ""
    private val focusMessageId: String = savedStateHandle["focusMessageId"] ?: ""

    init {
        /**
         * 先创建 Room 的会话父记录，再启动所有聊天数据流。
         * 这保证从好友页直接进入聊天时，历史消息不会因外键缺少父记录而写入失败。
         */
        viewModelScope.launch {
            if (conversationId.isBlank()) {
                updateState { it.copy(isLoading = false, historyErrorMessage = "会话参数无效，请返回后重试") }
                return@launch
            }
            chatRepository.ensureConversation(conversationId, title)
            observeLocalMessages()
            loadHistoryMessages()
            if (focusMessageId.isNotBlank()) loadMessageContext(focusMessageId)
            observeRealtimeMessages()
            chatRepository.connect()
        }
    }

    override fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.InputChanged -> updateState { it.copy(inputText = action.value) }
            is ChatAction.AttachmentSelected -> selectAttachment(action.uri)
            is ChatAction.RemoveDraftAttachment -> removeDraftAttachment(action.attachmentId)
            is ChatAction.ReplyTo -> replyTo(action.message)
            ChatAction.CancelReply -> updateState { it.copy(replyingTo = null) }
            ChatAction.SendMessage -> sendMessage()
            is ChatAction.RetryMessage -> retryMessage(action.message)
            ChatAction.RetryHistory -> retryHistory()
        }
    }

    private fun loadHistoryMessages() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, historyErrorMessage = null) }
            chatRepository.syncHistoryMessages(conversationId)
                .onSuccess { updateState { state -> state.copy(isLoading = false, historyErrorMessage = null) } }
                .onFailure { error ->
                    updateState { state ->
                        state.copy(
                            isLoading = false,
                            historyErrorMessage = error.message?.takeIf(String::isNotBlank) ?: "聊天记录加载失败，请重试",
                        )
                    }
                }
        }
    }

    /** 历史同步失败后的显式重试入口；Room 已有缓存会保留，不会在重试前清空。 */
    private fun retryHistory() {
        if (currentState.isLoading || conversationId.isBlank()) return
        loadHistoryMessages()
    }

    /** 搜索结果回跳时补齐目标消息上下文，Room 发射后页面会显示该历史片段。 */
    private fun loadMessageContext(messageId: String) {
        viewModelScope.launch { chatRepository.syncMessageContext(conversationId, messageId) }
    }

    /**
     * 先订阅 Room，再执行网络同步，保证断网或 WebSocket 不可用时仍优先显示本地历史消息。
     * 消息写入均由 Repository 完成，UI 不再维护与数据库分离的第二份列表。
     */
    private fun observeLocalMessages() {
        viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { messages ->
                updateState { it.copy(messages = messages) }
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

    /** 图片或文件选择后先上传，成功元数据进入草稿；失败通过 effect 提示且不产生失效附件。 */
    private fun selectAttachment(uri: Uri) {
        val state = currentState
        if (state.isUploadingAttachment || state.draftAttachments.size >= MAX_ATTACHMENTS) return
        viewModelScope.launch {
            updateState { it.copy(isUploadingAttachment = true) }
            chatRepository.uploadAttachment(conversationId, uri).onSuccess { attachment ->
                updateState { current ->
                    current.copy(
                        draftAttachments = (current.draftAttachments + attachment).distinctBy(ChatAttachment::id).take(MAX_ATTACHMENTS),
                        isUploadingAttachment = false,
                    )
                }
            }.onFailure { error ->
                updateState { it.copy(isUploadingAttachment = false) }
                sendEffect(ChatEffect.ShowError(error.message ?: "附件上传失败"))
            }
        }
    }

    private fun removeDraftAttachment(attachmentId: String) {
        val attachment = currentState.draftAttachments.firstOrNull { it.id == attachmentId } ?: return
        updateState { state -> state.copy(draftAttachments = state.draftAttachments.filterNot { it.id == attachmentId }) }
        viewModelScope.launch {
            chatRepository.discardAttachment(conversationId, attachment.id).onFailure { error ->
                sendEffect(ChatEffect.ShowError(error.message ?: "附件清理失败"))
            }
        }
    }

    /** 长按消息建立回复快照；发送时只提交原消息 ID，服务端重新生成可信展示快照。 */
    private fun replyTo(message: ChatMessage) {
        val preview = message.content.ifBlank { if (message.attachments.firstOrNull()?.type?.name == "IMAGE") "[图片]" else "[文件]" }
        updateState { it.copy(replyingTo = ChatMessageReply(message.id, if (message.isMine) "我" else message.avatarText.ifBlank { "对方" }, preview.take(120))) }
    }

    /**
     * 先插入 PENDING 消息，再用服务端返回值替换；失败时保留消息并标记 FAILED，避免用户输入丢失。
     */
    private fun sendMessage() {
        val draftState = currentState
        val content = draftState.inputText.trim()
        if ((content.isEmpty() && draftState.draftAttachments.isEmpty()) || draftState.isUploadingAttachment) return
        viewModelScope.launch {
            val pendingMessage = ChatMessage(
                id = "local-${UUID.randomUUID()}",
                conversationId = conversationId,
                content = content,
                timeLabel = "刚刚",
                isMine = true,
                avatarText = "我",
                timestamp = System.currentTimeMillis(),
                type = when {
                    draftState.draftAttachments.isEmpty() -> ChatMessageType.TEXT
                    draftState.draftAttachments.all { it.type.name == "IMAGE" } -> ChatMessageType.IMAGE
                    else -> ChatMessageType.FILE
                },
                attachments = draftState.draftAttachments,
                replyTo = draftState.replyingTo,
                sendStatus = ChatSendStatus.PENDING,
            )
            updateState { it.copy(inputText = "", draftAttachments = emptyList(), replyingTo = null, isSending = true) }
            sendPendingMessage(pendingMessage)
        }
    }

    /**
     * 重试只针对 Room 中已失败的本地消息，保留原有内容、附件和回复关系。
     * 执行流程：先将状态恢复为 PENDING，再复用发送流程；成功删除临时记录，失败重新标记 FAILED。
     */
    private fun retryMessage(message: ChatMessage) {
        if (message.sendStatus != ChatSendStatus.FAILED) return
        viewModelScope.launch {
            updateState { it.copy(isSending = true) }
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
            ),
            clientMessageId = pendingMessage.id,
        ).onSuccess {
            chatRepository.removeLocalMessage(pendingMessage.id)
            updateState { it.copy(isSending = false) }
        }.onFailure { error ->
            chatRepository.saveLocalMessage(pendingMessage.copy(sendStatus = ChatSendStatus.FAILED))
            updateState { it.copy(isSending = false) }
            sendEffect(ChatEffect.ShowError(error.message ?: "消息发送失败"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnect()
    }

    private companion object { const val MAX_ATTACHMENTS = 3 }
}
