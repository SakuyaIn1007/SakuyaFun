package com.sakuya.reader.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.sakuya.ui.theme.SakuyaInAndroidTheme

/**
 * ReaderTopBar.kt
 * 职责说明：只承载返回与「更多」菜单入口，并接收 ReaderScreen 映射后的主题语义色。
 * 执行流程：ReaderScreen 根据 ReaderTheme 提供颜色与动作 -> 顶栏只渲染并向上回传点击事件，
 * 不展示书名（正文区已足够辨识），也不直接触碰导航、书签或阅读进度数据。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    onBack: () -> Unit,
    showNavigationIcon: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    actions: @Composable RowScope.() -> Unit = {},
) {
    ReaderTopBarContent(
        onBack = onBack,
        showNavigationIcon = showNavigationIcon,
        containerColor = containerColor,
        contentColor = contentColor,
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBarContent(
    onBack: () -> Unit,
    showNavigationIcon: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        // 空标题占位：顶栏刻意不显示书名，仅靠正文与菜单辨识。
        title = {},
        navigationIcon = {
            if (showNavigationIcon) {
                /** 返回由 ReaderNavigation 传入 NavController.popBackStack，远端章节会回到卷章目录。 */
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor,
        ),
        actions = actions,
    )
}

@Preview(showBackground = true)
@Composable
fun ReaderTopBarPreview() {
    SakuyaInAndroidTheme(true) {
        ReaderTopBarContent(
            onBack = {},
            showNavigationIcon = true,
            actions = { Text("⋮") },
        )
    }
}
