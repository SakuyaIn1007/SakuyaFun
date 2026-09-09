package com.sakuya.conversation.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sakuya.model.chat.ChatAttachment
import com.sakuya.model.chat.ChatAttachmentType
import com.sakuya.model.chat.ChatMessage
import com.sakuya.model.chat.ChatMessageReply
import com.sakuya.model.chat.ChatReadStatus
import com.sakuya.model.chat.ChatSendStatus
import com.sakuya.conversation.ui.components.ChatInputBar
import com.sakuya.conversation.viewmodel.ChatAction
import com.sakuya.conversation.viewmodel.ChatEffect
import com.sakuya.conversation.viewmodel.ChatViewModel
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.motion.MotionContent
import com.sakuya.ui.motion.rememberMotionPreferences
import com.sakuya.ui.theme.SakuyaInAndroidTheme

/**
 * ChatScreen.kt
 * 职责说明：
 * 1. 收集 ChatViewModel 的不可变状态并将用户操作回传给 ViewModel。
 * 2. 只渲染 Room 驱动的聊天历史、发送状态和失败消息的重试入口。
 * 执行流程：Room 更新触发状态重组；WebSocket 失败不在页面提示，底层静默重连，
 * 因而离线时仍优先展示已缓存的历史消息。
 */
@Composable
fun ChatScreen(
    onBack: () -> Unit = {},
    onOpenDetail: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatEffect.ShowError -> errorMessage = effect.message
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onAction(ChatAction.AttachmentSelected(it)) }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onAction(ChatAction.AttachmentSelected(it)) }
    }

    ChatContent(
        title = uiState.title,
        messages = uiState.messages,
        isLoading = uiState.isLoading,
        focusMessageId = uiState.focusMessageId,
        inputText = uiState.inputText,
        draftAttachments = uiState.draftAttachments,
        replyingTo = uiState.replyingTo,
        isUploadingAttachment = uiState.isUploadingAttachment,
        errorMessage = errorMessage,
        historyErrorMessage = uiState.historyErrorMessage,
        onInputChanged = { viewModel.onAction(ChatAction.InputChanged(it)) },
        onPickImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onPickFile = { filePicker.launch(arrayOf("application/pdf", "text/plain", "application/zip", "application/epub+zip")) },
        onRemoveAttachment = { viewModel.onAction(ChatAction.RemoveDraftAttachment(it)) },
        onCancelReply = { viewModel.onAction(ChatAction.CancelReply) },
        onReplyMessage = { viewModel.onAction(ChatAction.ReplyTo(it)) },
        onSendMessage = { viewModel.onAction(ChatAction.SendMessage) },
        onRetryMessage = { viewModel.onAction(ChatAction.RetryMessage(it)) },
        onRetryHistory = { viewModel.onAction(ChatAction.RetryHistory) },
        onBack = onBack,
        onOpenDetail = onOpenDetail,
    )
}

@Composable
fun ChatContent(
    title: String,
    messages: List<ChatMessage>,
    isLoading: Boolean,
    focusMessageId: String = "",
    inputText: String,
    draftAttachments: List<ChatAttachment>,
    replyingTo: ChatMessageReply?,
    isUploadingAttachment: Boolean,
    errorMessage: String?,
    historyErrorMessage: String?,
    onInputChanged: (String) -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onCancelReply: () -> Unit,
    onReplyMessage: (ChatMessage) -> Unit,
    onSendMessage: () -> Unit,
    onRetryMessage: (ChatMessage) -> Unit,
    onRetryHistory: () -> Unit,
    onBack: () -> Unit,
    onOpenDetail: () -> Unit = {},
) {
    val motionPreferences = rememberMotionPreferences()
    val listState = rememberLazyListState()
    var locatedFocusMessageId by rememberSaveable(focusMessageId) { mutableStateOf("") }
    LaunchedEffect(focusMessageId, messages) {
        val index = findFocusMessageIndex(messages, focusMessageId)
        // 上下文首次进入 Room 后只定位一次，避免实时新消息到达时把用户反复拉回历史位置。
        if (index >= 0 && locatedFocusMessageId != focusMessageId) {
            listState.animateScrollToItem(index)
            locatedFocusMessageId = focusMessageId
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppSecondaryTopBar(title = title, onBack = onBack, actions = {
                IconButton(onClick = onOpenDetail) { Icon(Icons.Default.MoreVert, contentDescription = "聊天详情") }
            })
        },
        bottomBar = {
            ChatInputBar(
                value = inputText,
                attachments = draftAttachments,
                replyingTo = replyingTo,
                isUploadingAttachment = isUploadingAttachment,
                onValueChange = onInputChanged,
                onPickImage = onPickImage,
                onPickFile = onPickFile,
                onRemoveAttachment = onRemoveAttachment,
                onCancelReply = onCancelReply,
                onSendMessage = onSendMessage
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = listState,
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && isLoading) {
                item(key = "loading_messages") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
            if (messages.isEmpty() && !isLoading && historyErrorMessage == null) {
                item(key = "empty_messages") {
                    Text(
                        text = "暂无聊天记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            }
            if (errorMessage != null) {
                item(key = "error") {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            if (historyErrorMessage != null) {
                item(key = "history_error") {
                    Text(
                        text = historyErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Text(
                        text = "点击重新加载聊天记录",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRetryHistory)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }
            items(messages, key = { it.id }) { message ->
                MessageRow(
                    message = message,
                    onRetry = { onRetryMessage(message) },
                    onReply = { onReplyMessage(message) },
                    isHighlighted = isFocusMessage(message.id, focusMessageId),
                    modifier = if (motionPreferences.animationsEnabled) Modifier.animateItem() else Modifier,
                )
            }
        }
    }
}

/**
 * 计算历史搜索回跳目标在 Room 时间升序消息列表中的位置。
 * 返回 -1 表示上下文尚未写入，调用方应等待下一次 Room 发射后重试定位。
 */
internal fun findFocusMessageIndex(messages: List<ChatMessage>, focusMessageId: String): Int =
    if (focusMessageId.isBlank()) -1 else messages.indexOfFirst { it.id == focusMessageId }

/** 只有非空且 ID 完全匹配的目标消息才使用高亮样式，避免空参数误高亮。 */
internal fun isFocusMessage(messageId: String, focusMessageId: String): Boolean =
    focusMessageId.isNotBlank() && messageId == focusMessageId

@Composable
private fun MessageRow(
    message: ChatMessage,
    onRetry: () -> Unit,
    onReply: () -> Unit,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isMine) {
            ChatAvatar(text = message.avatarText.ifEmpty { "?" })
            Spacer(modifier = Modifier.width(8.dp))
        }
        MessageBubble(message = message, onRetry = onRetry, onReply = onReply, isHighlighted = isHighlighted)
        if (message.isMine) {
            Spacer(modifier = Modifier.width(8.dp))
            ChatAvatar(text = "我", isMine = true)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    onRetry: () -> Unit,
    onReply: () -> Unit,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isHighlighted) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else if (message.isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else if (message.isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Column(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(onClick = {}, onLongClick = onReply)
            .background(bubbleColor)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        message.replyTo?.let { reply ->
            Text(
                text = "回复 ${reply.senderName}：${reply.preview}",
                color = textColor.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        message.attachments.forEach { attachment ->
            if (attachment.type == ChatAttachmentType.IMAGE) {
                AsyncImage(
                    model = attachment.thumbnailUrl ?: attachment.url,
                    contentDescription = attachment.name.ifBlank { "聊天图片" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().size(width = 240.dp, height = 160.dp).clip(RoundedCornerShape(4.dp)).padding(bottom = 4.dp),
                )
            } else {
                Text("[文件] ${attachment.name.ifBlank { "附件" }}", color = textColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        if (message.content.isNotBlank()) Text(text = message.content, color = textColor, style = MaterialTheme.typography.bodyMedium)
        if (message.isMine) {
            /** 发送状态由 ViewModel 驱动；内容切换只反馈状态，不会重新播放历史消息动画。 */
            MotionContent(targetState = message.sendStatus) { sendStatus ->
                Text(
                    text = when (sendStatus) {
                        ChatSendStatus.PENDING -> "发送中"
                        ChatSendStatus.FAILED -> "发送失败，点击重试"
                        ChatSendStatus.SENT -> if (message.readStatus == ChatReadStatus.READ) "已读" else "已送达"
                    },
                    color = textColor.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 3.dp)
                        .then(
                            if (sendStatus == ChatSendStatus.FAILED) Modifier.clickable(onClick = onRetry) else Modifier
                        ),
                )
            }
        }
    }
}

@Composable
private fun ChatAvatar(
    text: String,
    modifier: Modifier = Modifier,
    isMine: Boolean = false
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isMine) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isMine) {
                MaterialTheme.colorScheme.onTertiary
            } else {
                MaterialTheme.colorScheme.onPrimary
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatContentPreview() {
    SakuyaInAndroidTheme(true) {
        ChatContent(
            title = "十六夜咲夜",
            messages = listOf(
                ChatMessage(
                    id = "msg-1",
                    conversationId = "sakuya",
                    content = "今天要确认一下资料。",
                    timeLabel = "今天 17:02",
                    isMine = false,
                    avatarText = "咲"
                ),
                ChatMessage(
                    id = "msg-2",
                    conversationId = "sakuya",
                    content = "收到，我等下补两项。",
                    timeLabel = "17:09",
                    isMine = true
                ),
            ),
            isLoading = false,
            focusMessageId = "",
            inputText = "",
            draftAttachments = emptyList(),
            replyingTo = null,
            isUploadingAttachment = false,
            errorMessage = null,
            historyErrorMessage = null,
            onInputChanged = {},
            onPickImage = {},
            onPickFile = {},
            onRemoveAttachment = {},
            onCancelReply = {},
            onReplyMessage = {},
            onSendMessage = {},
            onRetryMessage = {},
            onRetryHistory = {},
            onBack = {},
            onOpenDetail = {},
        )
    }
}
