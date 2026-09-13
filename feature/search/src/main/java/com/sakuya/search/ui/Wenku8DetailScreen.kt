package com.sakuya.search.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakuya.data.content.ContentChapterDto
import com.sakuya.data.content.ContentChapterIndexDto
import com.sakuya.data.content.ContentNovelDto
import com.sakuya.data.content.ContentVolumeDto
import com.sakuya.data.content.resolveContentUrl
import com.sakuya.search.viewmodel.Wenku8DetailUiState
import com.sakuya.search.viewmodel.Wenku8DetailViewModel
import coil.compose.AsyncImage

/**
 * Wenku8DetailScreen.kt
 * 职责说明：展示后端内容库中的小说信息，并提供「整本阅读」与「章节阅读」两种进入方式。
 * 执行流程：进页只读元数据 -> 用户选整本阅读直接进入连续阅读 -> 选章节阅读才加载并就地展开目录 -> 点某章进入阅读。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Wenku8DetailScreen(
    onBack: () -> Unit,
    onChapterClick: (String, String) -> Unit,
    onFullReadClick: () -> Unit,
    viewModel: Wenku8DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Wenku8DetailContent(
        state = state,
        onBack = onBack,
        onChapterClick = onChapterClick,
        onFullReadClick = onFullReadClick,
        onSelectChapterMode = viewModel::selectChapterMode,
        onCollapseChapterMode = viewModel::collapseChapterMode,
        onRetryIndex = viewModel::loadIndex,
        onRetry = viewModel::loadMetadata,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Wenku8DetailContent(
    state: Wenku8DetailUiState,
    onBack: () -> Unit,
    onChapterClick: (String, String) -> Unit,
    onFullReadClick: () -> Unit,
    onSelectChapterMode: () -> Unit,
    onCollapseChapterMode: () -> Unit,
    onRetryIndex: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        state.novel?.title ?: "小说详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Column(
                Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text("重试") }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
                item { BookHeader(state) }
                item { ReadModeActions(state, onFullReadClick, onSelectChapterMode, onCollapseChapterMode) }
                // 目录仅在用户选择「章节阅读」后展开；展开时才触发下载，避免进页即抓取整本。
                if (state.chapterMode) {
                    when {
                        state.indexLoading -> item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                            }
                        }
                        state.indexError != null -> item {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("目录加载失败：${state.indexError}", color = MaterialTheme.colorScheme.error)
                                OutlinedButton(onClick = onRetryIndex) { Text("重新加载目录") }
                            }
                        }
                        else -> {
                            val volumes = state.index?.volumes.orEmpty()
                            if (volumes.isEmpty()) {
                                item {
                                    Text(
                                        "暂无目录信息。",
                                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            volumes.forEach { volume ->
                                item {
                                    Text(
                                        volume.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
                                    )
                                }
                                items(volume.chapters, key = { it.id }) { chapter ->
                                    Text(
                                        chapter.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onChapterClick(chapter.id, chapter.title) }
                                            .padding(horizontal = 20.dp, vertical = 14.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 封面按竖版比例展示：横向拉满会把 2:3 的竖版封面裁掉大半，故限制宽度并居中。 */
@Composable
private fun BookHeader(state: Wenku8DetailUiState) {
    val novel = state.novel
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = resolveContentUrl(novel?.coverUrl),
                contentDescription = "${novel?.title.orEmpty()} 封面",
                modifier = Modifier
                    .height(240.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            novel?.title.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            "${novel?.author.orEmpty()}${novel?.status?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (novel?.copyright == true) {
            Text(
                "该作品受版权限制，部分章节可能不可阅读。",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (!novel?.tags.isNullOrEmpty()) {
            Text(
                novel!!.tags.joinToString(" · "),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(novel?.description.orEmpty(), modifier = Modifier.padding(top = 16.dp))
    }
}

/**
 * 两种阅读方式。整本阅读直接进入连续阅读；章节阅读就地展开目录，
 * 用户点具体章节后才请求该章内容。
 */
@Composable
private fun ReadModeActions(
    state: Wenku8DetailUiState,
    onFullReadClick: () -> Unit,
    onSelectChapterMode: () -> Unit,
    onCollapseChapterMode: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onFullReadClick, modifier = Modifier.weight(1f)) { Text("整本阅读") }
            // 用文字箭头而非图标：方向箭头属于 material-icons-extended，
            // 为两个装饰性图标引入整个依赖包不划算。
            OutlinedButton(
                onClick = if (state.chapterMode) onCollapseChapterMode else onSelectChapterMode,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.chapterMode) "收起目录 ▲" else "章节阅读 ▼")
            }
        }
        AnimatedVisibility(visible = state.chapterMode) {
            Text(
                "选择章节开始阅读",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

private fun previewNovel() = ContentNovelDto(
    id = "wenku8-1",
    title = "文学少女",
    author = "野村美月",
    description = "以「文学少女」远子学姐为主角的系列作品，围绕着古今中外的文学作品展开。",
    status = "连载中",
    tags = listOf("校园", "推理", "轻小说"),
)

private fun previewIndex() = ContentChapterIndexDto(
    bookId = "wenku8-1",
    volumes = listOf(
        ContentVolumeDto(
            id = "v0",
            title = "第一卷 渴望死亡的小丑",
            chapters = listOf(
                ContentChapterDto("0", "序章 取代自我介绍的回忆", 0),
                ContentChapterDto("85", "第一章 远子学姐是位美食家", 1),
                ContentChapterDto("332", "第二章 这个世界上最美味的故事", 2),
            ),
        ),
    ),
)

/** 元数据态：封面按竖版比例展示，两个阅读方式按钮并排。 */
@Preview(showBackground = true, name = "详情页 - 仅元数据")
@Composable
private fun Wenku8DetailMetadataPreview() {
    Wenku8DetailContent(
        state = Wenku8DetailUiState(novel = previewNovel(), loading = false),
        onBack = {}, onChapterClick = { _, _ -> }, onFullReadClick = {},
        onSelectChapterMode = {}, onCollapseChapterMode = {}, onRetryIndex = {}, onRetry = {},
    )
}

/** 章节阅读态：目录就地展开在按钮下方。 */
@Preview(showBackground = true, name = "详情页 - 章节阅读已展开")
@Composable
private fun Wenku8DetailChapterModePreview() {
    Wenku8DetailContent(
        state = Wenku8DetailUiState(
            novel = previewNovel(), index = previewIndex(),
            loading = false, chapterMode = true,
        ),
        onBack = {}, onChapterClick = { _, _ -> }, onFullReadClick = {},
        onSelectChapterMode = {}, onCollapseChapterMode = {}, onRetryIndex = {}, onRetry = {},
    )
}
