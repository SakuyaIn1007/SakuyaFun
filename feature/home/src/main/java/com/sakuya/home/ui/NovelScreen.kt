package com.sakuya.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sakuya.home.ui.components.ContentCard
import com.sakuya.ui.theme.SakuyaInAndroidTheme

@Composable
fun NovelScreen() {
}

@Composable
fun NovelContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "轻小说精选",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        val items = listOf(
            Triple("86-不存在的战区", "安里アサト · 电击文库", 4.8f to listOf("科幻", "战争", "机甲")),
            Triple("魔法科高校的劣等生", "佐岛勤 · 电击文库", 4.4f to listOf("科幻", "校园", "魔法")),
            Triple("古书堂事件手帖", "三上延 · MediaWorks", 4.3f to listOf("悬疑", "治愈", "日常")),
            Triple("狼与香辛料", "支仓冻砂 · 电击文库", 4.7f to listOf("冒险", "经商", "奇幻")),
            Triple("物语系列", "西尾维新 · 讲谈社BOX", 4.6f to listOf("怪谈", "悬疑", "青春")),
            Triple("NO GAME NO LIFE", "榎宫祐 · MF文库J", 4.5f to listOf("异世界", "智斗", "奇幻")),
            Triple("文豪野犬", "朝雾卡夫卡 · 角川Beans", 4.3f to listOf("超能力", "悬疑", "文学")),
            Triple("地错-在地下城寻求邂逅", "大森藤野 · GA文库", 4.5f to listOf("冒险", "奇幻", "恋爱")),
            Triple("Fate/Zero", "虚渊玄 · TYPE-MOON", 4.8f to listOf("圣杯战争", "黑暗", "史诗")),
            Triple("樱花庄的宠物女孩", "鸭志田一 · 电击文库", 4.4f to listOf("恋爱", "青春", "校园")),
        )

        items.forEachIndexed { index, item ->
            ContentCard(
                title = item.first,
                subtitle = item.second,
                rating = item.third.first,
                tags = item.third.second,
                index = index,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
fun NovelPreview() {
    SakuyaInAndroidTheme {
        NovelContent()
    }
}
