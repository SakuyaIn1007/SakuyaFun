package com.sakuya.catalog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakuya.catalog.viewmodel.CatalogViewModel
import com.sakuya.catalog.model.ContentItem

/**
 * 轻小说大赏独立路由页。
 * 页面负责获取 ViewModel 状态，纯展示继续交给 LightNovelAwardContent。
 */
@Composable
fun LightNovelAwardScreen(
    onBack: () -> Unit,
    onBookClick: (ContentItem) -> Unit,
) {
    val viewModel: CatalogViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CatalogSecondaryScaffold(title = "轻小说大赏", onBack = onBack) {
        LightNovelAwardContent(
            items = uiState.recommendItems + uiState.novelItems,
            onBookClick = onBookClick,
        )
    }
}
