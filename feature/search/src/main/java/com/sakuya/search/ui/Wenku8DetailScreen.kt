package com.sakuya.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakuya.search.viewmodel.Wenku8DetailViewModel
import com.sakuya.data.BuildConfig
import coil.compose.AsyncImage

/**
 * Wenku8DetailScreen.kt
 * 职责说明：展示带来源标识的 Wenku8 小说信息和分卷目录；不提供加入本地或云端书架的入口。
 * 执行流程：ViewModel 加载详情/目录 -> 用户点击章节 -> 导航层创建远端章节阅读会话。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Wenku8DetailScreen(onBack: () -> Unit, onChapterClick: (String, String) -> Unit, viewModel: Wenku8DetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    /** 详情页必须始终提供返回入口；目录和正文分别属于后续导航层级。 */
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(state.novel?.title ?: "小说详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.loading -> CircularProgressIndicator(modifier = Modifier.padding(innerPadding))
            state.error != null -> Column(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)) { Text(state.error!!, color = MaterialTheme.colorScheme.error); Button(onClick = viewModel::load) { Text("重试") } }
            else -> LazyColumn(Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)) {
            item {
                val novel = state.novel
                if (novel != null) {
                    AsyncImage(
                        model = "${BuildConfig.API_BASE_URL}wenku8/novels/${novel.id}/cover",
                        contentDescription = "${novel.title} 封面",
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(novel?.title.orEmpty(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
                Text("Wenku8 · ${novel?.author.orEmpty()}${novel?.status?.let { " · $it" }.orEmpty()}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
                if (novel?.copyright == true) Text("该作品受版权限制，部分章节可能不可阅读。", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                if (!novel?.tags.isNullOrEmpty()) Text(novel!!.tags.joinToString(" · "), color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 8.dp))
                Text(novel?.description.orEmpty(), modifier = Modifier.padding(vertical = 16.dp))
                Text("目录", style = MaterialTheme.typography.titleLarge)
            }
            state.index?.volumes?.forEach { volume ->
                item { Text(volume.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)) }
                items(volume.chapters, key = { it.id }) { chapter -> Text(chapter.title, modifier = Modifier.fillMaxWidth().clickable { onChapterClick(chapter.id, chapter.title) }.padding(vertical = 14.dp), style = MaterialTheme.typography.bodyLarge) }
            }
        }
        }
    }
}
