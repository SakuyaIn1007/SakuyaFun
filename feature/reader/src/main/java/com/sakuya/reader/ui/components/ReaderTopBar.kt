package com.sakuya.reader.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.ui.theme.SakuyaInAndroidTheme

/**
 * ReaderTopBar.kt
 * 职责说明：展示阅读器标题、返回和阅读会话操作，并接收 ReaderScreen 映射后的主题语义色。
 * 执行流程：ReaderScreen 根据 ReaderTheme 提供颜色与动作 -> 顶栏只渲染并向上回传点击事件，
 * 不直接触碰导航、书签或阅读进度数据。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    title: String,
    onBack: () -> Unit,
    showNavigationIcon: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    actions: @Composable RowScope.() -> Unit = {}
) {
    ReaderTopBarContent(
        title = title,
        onBack = onBack,
        showNavigationIcon = showNavigationIcon,
        containerColor = containerColor,
        contentColor = contentColor,
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBarContent(
    title: String,
    onBack: () -> Unit,
    showNavigationIcon: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    actions: @Composable RowScope.() -> Unit = {}
){
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
        } ,
        navigationIcon = {
            if (showNavigationIcon) {
                /** 返回由 ReaderNavigation 传入 NavController.popBackStack，远端章节会回到卷章目录。 */
                TextButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        modifier = Modifier.size(24.dp),
                        contentDescription = "返回"
                    )
                    Text("返回")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor,
        ),
        actions = actions
    )
}



@Preview(showBackground = true)
@Composable
fun ReaderTopBarPreview(){
    SakuyaInAndroidTheme(true) {
        ReaderTopBarContent(
            title = "123",
            onBack = {},
            showNavigationIcon = true
        )
    }
}
