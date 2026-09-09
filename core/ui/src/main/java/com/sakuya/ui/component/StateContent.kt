package com.sakuya.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.sakuya.ui.theme.SakuyaTheme

/**
 * StateContent.kt
 * 职责说明：统一页面数据加载、空数据和可恢复失败时的反馈层级。
 * 页面负责根据自身 UiState 选择状态组件；重试事件仍由页面上抛给 ViewModel 处理。
 */
@Composable
fun AppLoadingState(modifier: Modifier = Modifier, message: String = "正在加载内容") {
    val dimensions = SakuyaTheme.tokens.dimensions
    Column(
        modifier = modifier.fillMaxWidth().padding(dimensions.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(dimensions.spaceMd))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AppEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) = AppFeedbackState(title, description, modifier, actionLabel, onAction)

@Composable
fun AppErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) = AppFeedbackState("暂时无法加载", message, modifier, if (onRetry != null) "重新加载" else null, onRetry)

@Composable
private fun AppFeedbackState(
    title: String,
    description: String,
    modifier: Modifier,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    val dimensions = SakuyaTheme.tokens.dimensions
    Column(
        modifier = modifier.fillMaxWidth().padding(dimensions.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(dimensions.spaceSm))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(dimensions.spaceLg))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}
