package com.sakuya.feed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FeedTimeline(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        Text("暂无动态", style = MaterialTheme.typography.titleMedium)
        Text("发布一条动态，和大家分享你的阅读感受。", color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun ComposeFeedScreen(onPublished: () -> Unit) {
    var content by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
        Text("发表动态", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = content, onValueChange = { content = it }, placeholder = { Text("分享此刻的想法…") },
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp), minLines = 6)
        Button(onClick = onPublished, enabled = content.isNotBlank(), modifier = Modifier.padding(top = 16.dp)) { Text("发布") }
    }
}
