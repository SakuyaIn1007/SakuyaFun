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
fun MusicScreen() {
}

@Composable
fun MusicContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "音乐推荐",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        val items = listOf(
            Triple("Gurenge", "LiSA · 鬼灭之刃 OP", 4.9f to listOf("动漫", "热血", "摇滚")),
            Triple("IDOL", "YOASOBI · 我推的孩子 OP", 4.8f to listOf("动漫", "流行", "电子")),
            Triple("KICK BACK", "米津玄师 · 电锯人 OP", 4.7f to listOf("动漫", "摇滚", "另类")),
            Triple("カワキヲアメク", "美波 · 家有女友 OP", 4.6f to listOf("动漫", "摇滚", "J-Rock")),
            Triple("シルエット", "KANA-BOON · 火影忍者 OP", 4.7f to listOf("动漫", "摇滚", "J-Rock")),
            Triple("紅蓮華", "LiSA · 鬼灭之刃 OP", 4.9f to listOf("动漫", "热血", "摇滚")),
            Triple("夜に駆ける", "YOASOBI · 单曲", 4.8f to listOf("流行", "电子", "节奏")),
            Triple("廻廻奇譚", "Eve · 咒术回战 OP", 4.7f to listOf("动漫", "摇滚", "J-Rock")),
            Triple("新時代", "Ado · ONE PIECE RED", 4.6f to listOf("动漫", "流行", "J-Pop")),
            Triple("Cry Baby", "Official髭男dism · 东京卍复仇者", 4.5f to listOf("动漫", "流行", "J-Pop")),
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
fun MusicPreview() {
    SakuyaInAndroidTheme {
        MusicContent()
    }
}
