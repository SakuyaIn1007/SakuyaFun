package com.sakuya.search.ui

import androidx.compose.runtime.Composable

/**
 * SearchResultsScreen.kt
 * 职责说明：承载搜索结果页面，与搜索入口页解耦。
 * 执行流程：SearchNavigation 传入查询词 -> SearchResultsContent 维护筛选状态 -> 按结果类型回传书籍或动态 ID。
 */
@Composable
fun SearchResultsScreen(
    query: String,
    onBack: () -> Unit,
    onBookClick: (String) -> Unit = {},
    onDynamicClick: (String) -> Unit = {},
    onUserClick: (String) -> Unit = {},
) = SearchResultsContent(
    query = query,
    onBack = onBack,
    onBookClick = onBookClick,
    onDynamicClick = onDynamicClick,
    onUserClick = onUserClick,
)
