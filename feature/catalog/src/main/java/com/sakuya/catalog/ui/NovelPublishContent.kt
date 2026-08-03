package com.sakuya.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakuya.ui.theme.SakuyaInAndroidTheme

private data class DaySchedule(
    val dayOfWeek: String,
    val date: String,
    val isToday: Boolean,
    val items: List<String>
)

@Composable
fun NovelPublishScreen() {
    NovelPublishContent()
}

@Composable
fun NovelPublishContent() {
    var selectedDay by remember { mutableStateOf(2) }

    val schedule = listOf(
        DaySchedule("周一", "6月9日", false, listOf("转生王女与天才千金的魔法革命", "关于邻家的天使大人", "熊熊勇闯异世界")),
        DaySchedule("周二", "6月10日", false, listOf("NO GAME NO LIFE 第12卷", "实力至上主义的教室 第8卷", "Overlord 第15卷")),
        DaySchedule("周三", "6月11日", true, listOf("刀剑神域 Unital Ring Ⅶ", "为美好的世界献上祝福 第18卷", "古书堂事件手帖 第8卷", "86-不存在的战区 Ep.11")),
        DaySchedule("周四", "6月12日", false, listOf("魔法科高校的劣等生 第34卷", "盾之勇者成名录 第23卷")),
        DaySchedule("周五", "6月13日", false, listOf("狼与香辛料 Spring Log Ⅸ", "十二国记 第13卷", "无职转生 第27卷")),
        DaySchedule("周六", "6月14日", false, listOf("青春猪头少年系列 第15卷", "Re:从零开始的异世界生活 第39卷", "物语系列 Off Season 04")),
        DaySchedule("周日", "6月15日", false, listOf("Fate/Zero 文库版全3卷", "樱花庄的宠物女孩 第10.5卷")),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "刊发时间表",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(schedule) { index, day ->
                val isSelected = index == selectedDay

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else
                                MaterialTheme.colorScheme.surface
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = day.dayOfWeek,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )

                            if (day.isToday) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "今日",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        Text(
                            text = day.date,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        day.items.forEach { title ->
                            Text(
                                text = "✦  $title",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NovelPublishPreview() {
    SakuyaInAndroidTheme {
        NovelPublishContent()
    }
}
