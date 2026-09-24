package com.sakuya.profileservices.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sakuya.profileservices.viewmodel.StorageUsage
import com.sakuya.ui.component.AppSecondaryTopBar
import com.sakuya.ui.theme.SakuyaTheme

/**
 * StorageScreen.kt
 * 职责说明：展示各缓存分区占用并分级提供清理动作。
 * 执行流程：加载用量 -> 一键清理只处理可再生缓存；离线内容需二次确认后单独清理。
 *
 * 分级的原因：离线内容是用户主动下载的资产，不能混进一键清理里被顺手删掉。
 */
@Composable
fun StorageScreen(
    usage: StorageUsage,
    onBack: () -> Unit = {},
    onClearCaches: () -> Unit = {},
    onClearOffline: () -> Unit = {},
) {
    var confirmOffline by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppSecondaryTopBar(title = "缓存管理", onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Spacer(modifier = Modifier.height(SakuyaTheme.tokens.dimensions.spaceSm))
                Text(
                    text = "存储占用",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = SakuyaTheme.tokens.dimensions.spaceLg),
                )
                Spacer(modifier = Modifier.height(SakuyaTheme.tokens.dimensions.spaceSm))
            }

            item { StorageUsageRow("离线下载内容", usage.offlineBytes, "已下载的书籍正文，可离线阅读") }
            item { SettingItemDivider() }
            item { StorageUsageRow("内容缓存", usage.contentCacheBytes, "已读过的章节与目录") }
            item { SettingItemDivider() }
            item { StorageUsageRow("图片缓存", usage.imageCacheBytes, "封面与头像图片") }
            item { SettingItemDivider() }
            item { StorageUsageRow("临时文件", usage.temporaryBytes, "导入书籍时的中间产物") }

            item {
                Spacer(modifier = Modifier.height(SakuyaTheme.tokens.dimensions.spaceSm))
                Text(
                    text = "清理",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = SakuyaTheme.tokens.dimensions.spaceLg),
                )
                Spacer(modifier = Modifier.height(SakuyaTheme.tokens.dimensions.spaceSm))
            }

            item {
                SettingItemRow(
                    title = "清理缓存",
                    subtitle = formatBytes(usage.reclaimableBytes),
                    onClick = onClearCaches,
                )
            }
            item { SettingItemDivider() }
            item {
                SettingItemRow(
                    title = "移除全部离线内容",
                    subtitle = if (usage.offlineBytes > 0) formatBytes(usage.offlineBytes) else "暂无",
                    onClick = { confirmOffline = true },
                )
            }

            item {
                Text(
                    text = "清理缓存不会影响阅读进度、书签和离线内容；移除离线内容后这些也仍会保留。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = SakuyaTheme.tokens.dimensions.spaceLg, vertical = SakuyaTheme.tokens.dimensions.spaceMd),
                )
            }

            usage.message?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = SakuyaTheme.tokens.dimensions.spaceLg),
                    )
                }
            }
        }
    }

    if (confirmOffline) {
        AlertDialog(
            onDismissRequest = { confirmOffline = false },
            title = { Text("移除全部离线内容？") },
            text = { Text("将删除已下载的书籍正文，需要重新下载才能离线阅读。阅读进度和书签会保留。") },
            confirmButton = {
                TextButton(onClick = { confirmOffline = false; onClearOffline() }) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmOffline = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun StorageUsageRow(title: String, bytes: Long, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = SakuyaTheme.tokens.dimensions.spaceLg, vertical = SakuyaTheme.tokens.dimensions.spaceLg),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        Text(
            text = formatBytes(bytes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 统一按二进制单位展示，避免与系统设置的十进制单位产生观感差异。 */
internal fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(bytes / 1024f)
    bytes < 1024L * 1024L * 1024L -> "%.1f MB".format(bytes / 1024f / 1024f)
    else -> "%.2f GB".format(bytes / 1024f / 1024f / 1024f)
}
