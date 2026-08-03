package com.sakuya.conversation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
){
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
            .padding(4.dp), // 🌟 给整个工具栏加点边距
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = {},
            modifier = Modifier.size(32.dp) // 防误触大小
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                modifier = Modifier.size(24.dp),
                contentDescription = "更多选项",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // 🌟 核心修正：使用 BasicTextField 彻底解绑 M3 的 56dp 身材限制
        Box(
            modifier = Modifier
                .weight(1f)
                // 用外层 Box 绝对掌控输入框容器的上下高度喵！
                .heightIn(min = 36.dp)
                .background(
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(4.dp)
                )
                // 精准控制文字离输入框边界的内边距，使其完美垂直居中
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                maxLines = 1,
                // 继承你选定的 labelLarge 样式
                textStyle = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                // 改变光标颜色，防止在深色模式下看不清喵
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )

            // 🔍 巧妙实现占位符（Hint）
            if (value.isEmpty()) {
                Text(
                    text = "输入消息",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        // ➕ 表情/加号按钮
        IconButton(
            onClick = { /* 展开表情或面板 */ },
            modifier = Modifier.size(32.dp) // 防误触大小
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                modifier = Modifier.size(24.dp),
                contentDescription = "更多选项",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // 🚀 发送按钮（保留咲夜完美的联动与配色方案）
        val isHidden = value.isNotBlank()
        if(isHidden) {
            TextButton(
                onClick = onSendMessage,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.background,
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "发送",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatInputBarPreview(){
    SakuyaInAndroidTheme(true) {
        ChatInputBar(
            value = "明天见",
            onValueChange = {},
            onSendMessage = {}
        )
    }
}
