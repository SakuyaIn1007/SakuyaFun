package com.sakuya.profileservices.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.ui.component.AppSecondaryTopBar // 咲夜之前写好的二级 TopBar 喵
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onSwitchAccount: () -> Unit = {},
    onLogout: () -> Unit = {},
    onUnavailableClick: () -> Unit = {},
) {
    SettingsContent(
        onBack = onBack,
        onProfileClick = onProfileClick,
        onPrivacyClick = onPrivacyClick,
        onSwitchAccount = onSwitchAccount,
        onLogout = onLogout,
        onUnavailableClick = onUnavailableClick,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onSwitchAccount: () -> Unit = {},
    onLogout: () -> Unit = {},
    onUnavailableClick: () -> Unit = {},
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {
        // 🔙 1. 顶部返回导航栏
        AppSecondaryTopBar(
            title = "设置",
            onBack = onBack
        )

        // 📝 2. 设置项列表
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // ---- 🔐 第一组：账号与安全 ----

            item {
                Text(
                    text = "账号",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                SettingItemRow(
                    title = "个人资料",
                    onClick = onProfileClick
                )
            }
            item { SettingItemDivider() }
            item {
                SettingItemRow(
                    title = "账号安全",
                    subtitle = "暂未开放",
                    onClick = onUnavailableClick
                )
            }

            // ---- 🎨 第二组：通用与隐私 ----
            item {
                SettingGroupDivider()
                Text(
                    text = "功能",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } // 微信同款的分组灰色占位块喵
            item {
                SettingItemRow(
                    title = "新消息通知",
                    subtitle = "暂未开放",
                    onClick = onUnavailableClick
                )
            }
            item { SettingItemDivider() } // 同组内部的细分割线喵
            item {
                SettingItemRow(
                    title = "隐私",
                    onClick = onPrivacyClick
                )
            }
            item { SettingItemDivider() }
            item {
                SettingItemRow(
                    title = "通用",
                    subtitle = "暂未开放",
                    onClick = onUnavailableClick
                )
            }

            // ---- ℹ️ 第三组：关于与帮助 ----
            item {
                SettingGroupDivider()
                Text(
                    text = "帮助与关于",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                SettingItemRow(
                    title = "帮助与反馈",
                    subtitle = "暂未开放",
                    onClick = onUnavailableClick
                )
            }
            item { SettingItemDivider() }
            item {
                SettingItemRow(
                    title = "关于 SakuyaApp",
                    subtitle = "版本 1.0.0", // 顺便秀一下版本号喵
                    onClick = onUnavailableClick
                )
            }

            // ---- 🚪 第四组：退出登录 ----
            item { SettingGroupDivider() }
            item {
                SettingActionRow(
                    title = "切换账号",
                    onClick = onSwitchAccount
                )
            }
            item { SettingGroupDivider() }
            item {
                SettingActionRow(
                    title = "退出登录",
                    textColor = MaterialTheme.colorScheme.onBackground, // 退出登录用显眼的红色警告喵！
                    onClick = onLogout
                )
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

/**
 * 🛠️ 组件 A：标准的单条跳转设置项（左边文字，右边箭头）
 */
@Composable
fun SettingItemRow(
    title: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseOnSurface) // 设置项内部通常用亮色背景喵
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 🛠️ 组件 B：纯动作按钮项（比如居中对齐的“退出登录”）
 */
@Composable
fun SettingActionRow(
    title: String,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.inverseOnSurface)
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center, // 文字居中排布，更有仪式感喵
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

/**
 * 🛠️ 组件 C：同组内部的精细分割线
 */
@Composable
fun SettingItemDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp), // 左边缩进 16dp，右边顶格，这是最纯正的工业标准喵！
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    )
}

/**
 * 🛠️ 组件 D：不同业务组之间的灰色宽大占位块
 */
@Composable
fun SettingGroupDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) // 淡淡的灰色背景
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    SakuyaInAndroidTheme(darkTheme = true) {
        SettingsScreen()
    }
}
