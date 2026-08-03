package com.sakuya.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SearchScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("综合", "内容", "轻小说")

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("搜索内容或轻小说") }, modifier = Modifier.fillMaxWidth().padding(start = 48.dp)
            )
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, title ->
                Text(title, modifier = Modifier.clickable { selectedTab = index }.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.clickable { }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("☷", style = MaterialTheme.typography.titleLarge)
                Text("筛选", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
        SearchResults(query, selectedTab)
    }
}

@Composable
private fun SearchResults(query: String, selectedTab: Int) {
    val title = query.ifBlank { "开始搜索你感兴趣的内容" }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopCenter) {
        Column {
            if (selectedTab == 0 || selectedTab == 2) {
                Text("轻小说优先", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("《$title》", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("轻小说 · 最匹配的作品会优先展示", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
            }
            Text(if (query.isBlank()) "搜索历史和热门搜索将显示在这里" else "正在为“$query”整理相关结果",
                color = MaterialTheme.colorScheme.outline)
        }
    }
}
