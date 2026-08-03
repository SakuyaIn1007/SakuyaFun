package com.sakuya.conversation.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.conversation.data.remote.ChatMessageDto
import com.sakuya.conversation.ui.components.ChatInputBar
import com.sakuya.conversation.viewmodel.ChatViewModel
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun ChatScreen(
    onBack: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ChatContent(
        title = uiState.title,
        messages = uiState.messages,
        inputText = uiState.inputText,
        errorMessage = uiState.errorMessage,
        onInputChanged = viewModel::onInputChanged,
        onSendMessage = viewModel::sendMessage,
        onBack = onBack
    )
}

@Composable
fun ChatContent(
    title: String,
    messages: List<ChatMessageDto>,
    inputText: String,
    errorMessage: String?,
    onInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppSecondaryTopBar(
                title = title,
                onBack = onBack
            )
        },
        bottomBar = {
            ChatInputBar(
                value = inputText,
                onValueChange = onInputChanged,
                onSendMessage = onSendMessage
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (errorMessage != null) {
                item(key = "error") {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            items(messages, key = { it.id }) { message ->
                MessageRow(message = message)
            }
        }
    }
}

@Composable
private fun MessageRow(
    message: ChatMessageDto,
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
        MessageBubble(message = message)
        if (message.isMine) {
            Spacer(modifier = Modifier.width(8.dp))
            ChatAvatar(text = "我", isMine = true)
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageDto,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (message.isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.inverseOnSurface
    }
    val textColor = if (message.isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Column(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bubbleColor)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(
            text = message.content,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
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
                ChatMessageDto(
                    id = "msg-1",
                    conversationId = "sakuya",
                    content = "今天要确认一下资料。",
                    timeLabel = "今天 17:02",
                    isMine = false,
                    avatarText = "咲"
                ),
                ChatMessageDto(
                    id = "msg-2",
                    conversationId = "sakuya",
                    content = "收到，我等下补两项。",
                    timeLabel = "17:09",
                    isMine = true
                ),
            ),
            inputText = "",
            errorMessage = null,
            onInputChanged = {},
            onSendMessage = {},
            onBack = {}
        )
    }
}
