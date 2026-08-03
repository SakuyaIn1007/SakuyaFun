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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakuya.catalog.viewmodel.CatalogViewModel

@Composable
fun CatalogScheduleScreen(onBack: () -> Unit) {
    CatalogSecondaryScaffold(title = "时间表", onBack = onBack) {
        NovelPublishContent()
    }
}

@Composable
fun LightNovelAwardScreen(
    onBack: () -> Unit,
    onBookClick: (String) -> Unit
) {
    val viewModel: CatalogViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CatalogSecondaryScaffold(title = "轻小说大赏", onBack = onBack) {
        LightNovelAwardContent(
            items = uiState.recommendItems + uiState.novelItems,
            onBookClick = onBookClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogSecondaryScaffold(
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
