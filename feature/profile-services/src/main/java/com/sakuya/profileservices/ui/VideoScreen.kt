package com.sakuya.profileservices.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.sakuya.ui.theme.SakuyaInAndroidTheme

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 📅 1. 定义视频数据模型，带上时长和时间戳喵
data class VideoItem(
    val id: String,
    val videoUrl: String,   // 视频源地址喵
    val coverUrl: String,   // 视频封面图地址喵
    val durationMs: Long,   // 视频时长（毫秒数）
    val timestamp: Long     // 时间戳喵
) {
    // 采用和 PhotoItem 绝对一致的分组 Key 逻辑
    val yearMonth: String
        get() = SimpleDateFormat("yyyy年MM月", Locale.CHINESE).format(Date(timestamp))

    // 自动把毫秒数转换成漂亮的 “01:25” 微信同款小标签
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
}

@Composable
fun VideoScreen() {
    // 模拟视频数据喵（跨越不同月份）
    val mockVideos = listOf(
        // 2026年5月
        VideoItem("1", "", "https://picsum.photos/300?random=11", 15000L, 1747584000000L),
        VideoItem("2", "", "https://picsum.photos/300?random=12", 92000L, 1747670400000L),
        VideoItem("3", "", "https://picsum.photos/300?random=13", 125000L, 1747756800000L),
        VideoItem("4", "", "https://picsum.photos/300?random=14", 5000L, 1747843200000L),
        // 2026年4月
        VideoItem("5", "", "https://picsum.photos/300?random=15", 45000L, 1745000000000L),
        VideoItem("6", "", "https://picsum.photos/300?random=16", 218000L, 1745100000000L),
        // 2026年3月
        VideoItem("7", "", "https://picsum.photos/300?random=17", 8000L, 1742000000000L),
        VideoItem("8", "", "https://picsum.photos/300?random=18", 73000L, 1742100000000L),
        VideoItem("9", "", "https://picsum.photos/300?random=19", 14000L, 1742200000000L)
    )

    VideoContent(
        videos = mockVideos,
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalFoundationApi::class) // stickyHeader 刚性需要这个注解喵
@Composable
fun VideoContent(
    videos: List<VideoItem>,
    modifier: Modifier = Modifier
) {
    // 🌟 2. 核心魔法：一键打散成 Map<"xxxx年xx月", List<VideoItem>>
    val groupedVideos = videos.groupBy { it.yearMonth }

    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(4.dp) // 行与行之间的小间距
    ) {
        // 遍历分组后的 Map
        groupedVideos.forEach { (dateHeader, monthVideos) ->

            // 📌 3. 原汁原味的吸顶吸附标题喵！
            stickyHeader {
                Text(
                    text = dateHeader,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background) // 挡住滚过去的视频，防止重叠透光
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // 🧮 4. 严格保持一排 4 列网格的切片逻辑喵
            val columns = 3
            val chunkedRows = monthVideos.chunked(columns)

            items(chunkedRows.size) { rowIndex ->
                val rowVideos = chunkedRows[rowIndex]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp) // 格子水平间距
                ) {
                    // 渲染实际的视频格子喵
                    rowVideos.forEach { video ->
                        Box(
                            modifier = Modifier
                                .weight(1f) // 完美的 4 等分屏幕宽度
                                .aspectRatio(1f) // 强制 1:1 正方形
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            // 🖼️ 视频封面图
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(video.coverUrl)
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // ➕ 正中央半透明播放小图标，一眼看出是视频喵
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "播放",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.25f), shape = CircleShape)
                                    .padding(2.dp)
                            )

                            // 🕒 右下角小黑框时长标签
                            Text(
                                text = video.formattedDuration,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // 补位魔法：不满 4 张用空 Box 占坑，格子绝对不变形喵！
                    val emptySlots = columns - rowVideos.size
                    if (emptySlots > 0) {
                        repeat(emptySlots) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoGridItem(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f) // 1:1 正方形
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // 1. 视频封面
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.coverUrl)
                    .crossfade(true)
                    .build()
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. 正中央半透明播放图标
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "播放",
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.Center)
                .background(Color.Black.copy(alpha = 0.25f), shape = CircleShape)
                .padding(4.dp)
        )

        // 3. 右下角微信同款时长框
        Text(
            text = video.formattedDuration,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(2.dp)
                )
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VideoPreview() {
    SakuyaInAndroidTheme(darkTheme = false) {
        VideoScreen()
    }
}