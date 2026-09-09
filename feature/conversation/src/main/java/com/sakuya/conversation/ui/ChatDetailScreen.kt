package com.sakuya.conversation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.model.chat.ConversationDetail
import com.sakuya.model.chat.ConversationType
import com.sakuya.conversation.ui.components.ChatTopBar
import com.sakuya.conversation.viewmodel.ChatDetailAction
import com.sakuya.conversation.viewmodel.ChatDetailEffect
import com.sakuya.conversation.viewmodel.ChatDetailViewModel

/**
 * ChatDetailScreen.kt
 * 职责说明：渲染好友或群聊资料，以及当前用户可修改的会话偏好。
 * 执行流程：页面只收集 ViewModel 状态；开关、群名片、清空和退出均通过回调交给 ViewModel，
 * 从而保证 Compose 不直接执行网络或数据库操作。
 */
@Composable
fun ChatDetailScreen(onBack: () -> Unit, onSearchHistory: (String) -> Unit, onLeftGroup: () -> Unit, viewModel: ChatDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.hasLeftGroup) { if (state.hasLeftGroup) onLeftGroup() }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatDetailEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                ChatDetailEffect.LocalHistoryCleared -> snackbarHostState.showSnackbar("本地聊天记录已清空")
            }
        }
    }
    ChatDetailContent(
        detail = state.detail,
        isLoading = state.isLoading,
        isSaving = state.isSaving,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSearch = onSearchHistory,
        onPinned = { viewModel.onAction(ChatDetailAction.UpdatePinned(it)) },
        onMuted = { viewModel.onAction(ChatDetailAction.UpdateMuted(it)) },
        onMemberNickname = { viewModel.onAction(ChatDetailAction.UpdateMemberNickname(it)) },
        onClear = { viewModel.onAction(ChatDetailAction.ClearLocalHistory) },
        onLeave = { viewModel.onAction(ChatDetailAction.LeaveGroup) },
        onRetry = { viewModel.onAction(ChatDetailAction.Load) },
    )
}

@Composable
private fun ChatDetailContent(detail: ConversationDetail?, isLoading: Boolean, isSaving: Boolean, snackbarHostState: SnackbarHostState, onBack: () -> Unit, onSearch: (String) -> Unit, onPinned: (Boolean) -> Unit, onMuted: (Boolean) -> Unit, onMemberNickname: (String) -> Unit, onClear: () -> Unit, onLeave: () -> Unit, onRetry: () -> Unit) {
    Scaffold(topBar = { ChatTopBar(title = "聊天详情", onBack = onBack) }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            detail == null -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) { Text("聊天详情不可用", modifier = Modifier.padding(24.dp)); TextButton(onClick = onRetry) { Text("重试") } }
            else -> DetailList(detail, isSaving, { onSearch(detail.title) }, onPinned, onMuted, onMemberNickname, onClear, onLeave)
        }
    }
}

@Composable
private fun DetailList(detail: ConversationDetail, isSaving: Boolean, onSearch: () -> Unit, onPinned: (Boolean) -> Unit, onMuted: (Boolean) -> Unit, onMemberNickname: (String) -> Unit, onClear: () -> Unit, onLeave: () -> Unit) {
    var nickname by remember(detail.preferences.memberNickname) { mutableStateOf(detail.preferences.memberNickname) }
    var showClear by remember { mutableStateOf(false) }
    var showLeave by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(detail.title, style = MaterialTheme.typography.headlineSmall) }
        if (detail.type == ConversationType.DIRECT) {
            detail.directProfile?.let { profile -> item { InfoCard("好友资料") { Text(profile.nickname, style = MaterialTheme.typography.titleMedium); Text(if (profile.isOnline) "在线" else "离线", color = MaterialTheme.colorScheme.outline); if (!profile.signature.isNullOrBlank()) Text(profile.signature!!, modifier = Modifier.padding(top = 4.dp)) } } }
        } else {
            item { InfoCard("群聊资料") { if (detail.description.isNotBlank()) Text(detail.description); if (detail.announcement.isNotBlank()) Text("公告：${detail.announcement}", modifier = Modifier.padding(top = 6.dp)); Text("成员 ${detail.members.size} 人", modifier = Modifier.padding(top = 6.dp)) } }
            item { InfoCard("我的群名片") { OutlinedTextField(value = nickname, onValueChange = { nickname = it }, singleLine = true, label = { Text("群名片") }, modifier = Modifier.fillMaxWidth()); TextButton(onClick = { onMemberNickname(nickname) }, enabled = !isSaving) { Text("保存群名片") } } }
            item { Text("群成员", style = MaterialTheme.typography.titleSmall) }
            items(detail.members, key = { it.userId }) { member -> Text(member.displayName, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) }
        }
        item { Text("会话设置", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
        item { PreferenceSwitch("置顶会话", detail.preferences.isPinned, !isSaving, onPinned) }
        item { PreferenceSwitch("消息免打扰", detail.preferences.isMuted, !isSaving, onMuted) }
        item { ListAction("查找聊天记录", onSearch) }
        item { ListAction("清空本地聊天记录", { showClear = true }, isDestructive = true) }
        if (detail.type == ConversationType.GROUP) item { ListAction("退出群聊", { showLeave = true }, isDestructive = true) }
    }
    if (showClear) ConfirmDialog("清空本地聊天记录", "仅删除当前设备缓存，服务器消息不会删除。", { showClear = false }, { showClear = false; onClear() })
    if (showLeave) ConfirmDialog("退出群聊", "退出后将无法继续读取群消息。", { showLeave = false }, { showLeave = false; onLeave() })
}

@Composable private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) { Card { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(title, style = MaterialTheme.typography.titleSmall); Spacer(Modifier.height(8.dp)); content() } } }
@Composable private fun PreferenceSwitch(title: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = onChange, enabled = enabled) } }
@Composable private fun ListAction(title: String, onClick: () -> Unit, isDestructive: Boolean = false) { Text(title, color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)) }
@Composable private fun ConfirmDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) }, confirmButton = { TextButton(onClick = onConfirm) { Text("确认") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }) }
