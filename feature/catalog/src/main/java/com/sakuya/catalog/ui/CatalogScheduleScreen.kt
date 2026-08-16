package com.sakuya.catalog.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.sakuya.catalog.viewmodel.ScheduleViewModel
import com.sakuya.catalog.model.ContentItem

@Composable
fun CatalogScheduleScreen(
    onBack: () -> Unit,
    onBookClick: (ContentItem) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CatalogSecondaryScaffold(title = "时间表", onBack = onBack) {
        NovelPublishContent(uiState = uiState, onBookClick = onBookClick)
    }
}

/**
 * CatalogScheduleScreen.kt
 * 职责说明：承载轻小说刊发时间表的内容入口。
 * 执行流程：导航层打开时间表 -> ViewModel 提供已公布更新 -> SecondaryScaffold 承载推荐 Banner 与日期分组列表。
 */
@Composable
fun NovelPublishContent(
    uiState: com.sakuya.catalog.viewmodel.ScheduleUiState,
    onBookClick: (ContentItem) -> Unit,
) = CatalogScheduleContent(uiState, onBookClick)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CatalogSecondaryScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}
