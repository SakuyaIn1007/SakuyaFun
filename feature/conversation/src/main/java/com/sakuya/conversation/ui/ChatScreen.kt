package com.sakuya.conversation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.conversation.ui.components.ChatInputBar
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun ChatScreen(
    title: String,
    onBack: () -> Unit = {}
) {
    ChatContent(
        title = title,
        onBack = onBack
    )
}

@Composable
fun ChatContent(
    title: String = "十六夜咲夜",
    onBack: () -> Unit = {}
){
    var input by rememberSaveable { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>().apply { addAll(sampleMessages()) } }

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
                value = input,
                onValueChange = { input = it },
                onSendMessage = {
                    val content = input.trim()
                    if (content.isNotEmpty()) {
                        messages.add(
                            ChatMessage(
                                id = "local-${messages.size}",
                                content = content,
                                timeLabel = "刚刚",
                                isMine = true
                            )
                        )
                        input = ""
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                if (message.showTime) {
                    TimeDivider(timeLabel = message.timeLabel)
                }
                MessageRow(message = message)
            }
        }
    }
}

@Composable
private fun TimeDivider(
    timeLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = timeLabel,
            modifier = Modifier
                .background(Color.Transparent)
                .padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 12.dp),
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun MessageRow(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isMine) {
            ChatAvatar(text = message.avatarText)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
        }
        MessageBubble(message = message)
        if (message.isMine) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            ChatAvatar(text = "我", isMine = true)
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
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

private data class ChatMessage(
    val id: String,
    val content: String,
    val timeLabel: String,
    val isMine: Boolean,
    val avatarText: String = "咲",
    val showTime: Boolean = false
)

private fun sampleMessages() = listOf(
    ChatMessage(
        id = "time-1",
        content = "今天要确认一下资料。",
        timeLabel = "今天 17:02",
        isMine = false,
        showTime = true
    ),
    ChatMessage(
        id = "msg-2",
        content = "明天的清单我整理好了，你看一下有没有漏掉的地方。",
        timeLabel = "17:08",
        isMine = false
    ),
    ChatMessage(
        id = "msg-3",
        content = "收到，我等下补两项。",
        timeLabel = "17:09",
        isMine = true
    ),
    ChatMessage(
        id = "msg-4",
        content = "好，改完发我就行。",
        timeLabel = "17:10",
        isMine = false
    )
)

@Preview(showBackground = true)
@Composable
fun ChatPreview(){
    SakuyaInAndroidTheme(true) {
        ChatContent()
    }
}
