package com.sakuya.profileservices.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.sakuya.ui.theme.SakuyaInAndroidTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PhotoItem(
    val id: String,
    val url: String,
    val timestamp: Long
) {
    // 📅 获取这张照片的特定时间对象
    private val photoCalendar: Calendar = Calendar.getInstance().apply {
        time = Date(timestamp)
    }

    // 1. 提取出数字年份（例如：2026）
    val year: Int
        get() = photoCalendar.get(Calendar.YEAR)

    // 2. 提取出两位数的月份字符串（例如："05"、"12"）
    val month: String
        get() = String.format("%02d", photoCalendar.get(Calendar.MONTH) + 1)

    // 3. ✨ 核心判断：这张照片的年份是不是等于今年喵？
    val isCurrentYear: Boolean
        get() {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR) // 获取当前的真实年份
            return this.year == currentYear
        }

    // 4. 用来做分组的唯一 Key（哪怕年份不同、月份相同，也不会串在一起喵）
    val groupKey: String
        get() = "${year}年${month}月"
}

@Composable
fun PictureScreen() {
    // 模拟微信相册的假数据喵（跨越不同月份）
    val mockPhotos = listOf(
        // 2026年5月
        PhotoItem("1", "https://picsum.photos/300?random=1", 1747584000000L),
        PhotoItem("2", "https://picsum.photos/300?random=2", 1747670400000L),
        PhotoItem("3", "https://picsum.photos/300?random=3", 1747756800000L),
        PhotoItem("4", "https://picsum.photos/300?random=4", 1747843200000L),
        // 2026年4月
        PhotoItem("5", "https://picsum.photos/300?random=5", 1745000000000L),
        PhotoItem("6", "https://picsum.photos/300?random=6", 1745100000000L),
        // 2026年3月
        PhotoItem("7", "https://picsum.photos/300?random=7", 1742000000000L),
        PhotoItem("8", "https://picsum.photos/300?random=8", 1742100000000L),
        PhotoItem("9", "https://picsum.photos/300?random=9", 1742200000000L)
    )

    PictureContent(
        photos = mockPhotos,
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PictureContent(
    photos: List<PhotoItem>,
    modifier: Modifier = Modifier
) {
    // 1. 按年月份分组
    val groupedPhotos = photos.groupBy { it.groupKey }

    // 用来记录上一次渲染的年份，防止重复出现
    var lastRenderedYear: Int? = null

    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // 稍微缩小间距，靠年份和月份本身的排版来拉开层次喵
    ) {
        groupedPhotos.forEach { (groupKey, monthPhotos) ->
            val firstPhoto = monthPhotos.firstOrNull()
            val isCurrentYear = firstPhoto?.isCurrentYear ?: true
            val photoYear = firstPhoto?.year ?: 0
            val photoMonth = firstPhoto?.month ?: ""

            // 判断是否需要显示年份标题
            val shouldShowYearHeader = !isCurrentYear && photoYear != lastRenderedYear
            lastRenderedYear = photoYear

            // ========================================================
            // ✨ 修正点 1：把“年份”从 Row 里彻底揪出来，单独做成一个独立的列表项喵！
            // ========================================================
            if (shouldShowYearHeader) {
                item {
                    Text(
                        text = "${photoYear}年",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 16.dp, bottom = 8.dp) // 让年份上方有足够的跨年呼吸感
                    )
                }
            }

            // 📅 每一个月份和照片的组合包裹在一个独立的 item 里
            item {
                // 🌟 核心大容器：现在这个 Row 里面只有“月份”和“照片”平起平坐了喵！
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // 🕒 【左侧时间轴栏】：现在这里面清清爽爽，永远只有月份一行字喵！
                    Column(
                        modifier = Modifier.width(65.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = photoMonth + "月",
                                style = MaterialTheme.typography.titleLarge, // 月份大字
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 3.dp, start = 1.dp)
                            )
                        }
                    }

                    // 🧮 【右侧照片网格栏】：照片会完美的从月份的顶部平齐开始往下排喵
                    val columns = 3
                    val chunkedRows = monthPhotos.chunked(columns)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chunkedRows.forEach { rowPhotos ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowPhotos.forEach { photo ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(photo.url)
                                                    .crossfade(true)
                                                    .build()
                                            ),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                val emptySlots = columns - rowPhotos.size
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
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PicturePreview() {
    SakuyaInAndroidTheme(darkTheme = true) {
        PictureScreen()
    }
}