package com.sakuya.conversation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.model.chat.ChatAttachment
import com.sakuya.model.chat.ChatAttachmentType
import com.sakuya.model.chat.ChatMessageReply
import com.sakuya.ui.theme.SakuyaInAndroidTheme

/**
 * ChatInputBar.kt
 * 职责说明：渲染文字、已上传附件和回复草稿，并把图片/文件选择与发送事件上抛给页面。
 * 执行流程：选择媒体 -> ViewModel 上传 -> 成功附件进入本栏预览 -> 文字或附件非空时允许发送。
 */
@Composable
fun ChatInputBar(
    value: String,
    attachments: List<ChatAttachment>,
    replyingTo: ChatMessageReply?,
    isUploadingAttachment: Boolean,
    onValueChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onCancelReply: () -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding().imePadding().padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        replyingTo?.let { reply ->
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)).padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("回复 ${reply.senderName}：${reply.preview}", style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.weight(1f))
                IconButton(onClick = onCancelReply, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Close, contentDescription = "取消回复") }
            }
        }
        if (attachments.isNotEmpty() || isUploadingAttachment) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                attachments.forEach { attachment ->
                    Row(
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)).padding(start = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (attachment.type == ChatAttachmentType.IMAGE) Icons.Default.Add else Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(attachment.name.ifBlank { if (attachment.type == ChatAttachmentType.IMAGE) "图片" else "文件" }, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
                        IconButton(onClick = { onRemoveAttachment(attachment.id) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Close, contentDescription = "移除附件", modifier = Modifier.size(16.dp)) }
                    }
                }
                if (isUploadingAttachment) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onPickImage, enabled = !isUploadingAttachment && attachments.size < 3, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, contentDescription = "选择图片")
            }
            IconButton(onClick = onPickFile, enabled = !isUploadingAttachment && attachments.size < 3, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, contentDescription = "选择文件")
            }
            Box(
                modifier = Modifier.weight(1f).heightIn(min = 38.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
                if (value.isEmpty()) Text("输入消息", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
            if (value.isNotBlank() || attachments.isNotEmpty()) {
                TextButton(
                    onClick = onSendMessage, enabled = !isUploadingAttachment,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary, containerColor = MaterialTheme.colorScheme.primary),
                ) { Text("发送") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatInputBarPreview() {
    SakuyaInAndroidTheme(true) {
        ChatInputBar(
            value = "补充说明", attachments = emptyList(), replyingTo = ChatMessageReply("1", "咲夜", "请看这张图"),
            isUploadingAttachment = false, onValueChange = {}, onPickImage = {}, onPickFile = {},
            onRemoveAttachment = {}, onCancelReply = {}, onSendMessage = {},
        )
    }
}
