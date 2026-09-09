package com.sakuya.conversation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.model.chat.ChatMessage
import com.sakuya.conversation.ui.components.ChatTopBar
import com.sakuya.conversation.viewmodel.ChatHistorySearchAction
import com.sakuya.conversation.viewmodel.ChatHistorySearchEffect
import com.sakuya.conversation.viewmodel.ChatHistorySearchViewModel

/**
 * ChatHistorySearchScreen.kt
 * 职责说明：提供当前会话全文检索界面并将消息选择回传给导航层。
 * 执行流程：用户提交关键词 -> ViewModel 查询服务端 -> 点击结果携带消息 ID 回到聊天页加载上下文并定位。
 * 架构说明：MVI 风格，意图统一经 onAction 提交，一次性错误经 effect 收集后内联展示。
 */
@Composable
fun ChatHistorySearchScreen(onBack: () -> Unit, onMessageSelected: (String) -> Unit, viewModel: ChatHistorySearchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatHistorySearchEffect.ShowError -> errorMessage = effect.message
            }
        }
    }
    Scaffold(topBar = { ChatTopBar("查找聊天记录", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row { OutlinedTextField(value = state.query, onValueChange = { viewModel.onAction(ChatHistorySearchAction.QueryChanged(it)) }, label = { Text("关键词") }, singleLine = true, modifier = Modifier.weight(1f)); TextButton(onClick = { viewModel.onAction(ChatHistorySearchAction.Search) }, modifier = Modifier.padding(top = 8.dp)) { Text("搜索") } }
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.results, key = ChatMessage::id) { message -> SearchResult(message, onMessageSelected) } }
        }
    }
}
@Composable private fun SearchResult(message: ChatMessage, onSelected: (String) -> Unit) { Card(Modifier.fillMaxWidth().clickable { onSelected(message.id) }) { Column(Modifier.padding(12.dp)) { Text(message.timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline); Text(message.content, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp)) } } }
