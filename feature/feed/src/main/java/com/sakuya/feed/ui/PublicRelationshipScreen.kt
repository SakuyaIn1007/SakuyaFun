package com.sakuya.feed.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sakuya.feed.viewmodel.PublicRelationshipViewModel

/**
 * PublicRelationshipScreen.kt
 * 职责说明：展示指定他人主页的关注或粉丝名单。
 * 执行流程：路由传入 userId 和列表类型 -> ViewModel 请求分页接口 -> 以昵称与签名渲染当前页。
 */
@Composable
fun PublicRelationshipScreen(userId: String, followers: Boolean, viewModel: PublicRelationshipViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(userId, followers) { viewModel.load(userId, followers) }
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.errorMessage.orEmpty()) }
        else -> LazyColumn(Modifier.fillMaxSize()) {
            item { Text(if (followers) "粉丝" else "关注", Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge) }
            items(state.users, key = { it.userId }) { user ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text(user.name, style = MaterialTheme.typography.titleMedium)
                    Text(user.description.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
