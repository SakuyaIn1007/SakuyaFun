package com.sakuya.reader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sakuya.reader.data.EpubSearchResult

/** 搜索面板只渲染 ViewModel 已计算的结果；点击结果回传章节索引，不在组合期间解析正文。 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EpubSearchSheet(query: String, results: List<EpubSearchResult>, onQuery: (String) -> Unit, onResult: (EpubSearchResult) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(value = query, onValueChange = onQuery, label = { Text("搜索 EPUB 正文") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (query.trim().length >= 2 && results.isEmpty()) Text("未找到匹配内容", Modifier.padding(vertical = 24.dp))
            LazyColumn {
                items(results, key = { "${it.chapterIndex}:${it.matchOffset}" }) { result ->
                    Column(Modifier.fillMaxWidth().clickable { onResult(result) }.padding(vertical = 12.dp)) {
                        Text(result.chapterTitle, style = MaterialTheme.typography.titleSmall)
                        Text(result.excerpt, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
