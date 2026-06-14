package com.sakuya.home.data.datasource

import com.sakuya.home.model.ContentItem
import com.sakuya.home.model.RankTrend
import com.sakuya.home.model.RankingItem
import javax.inject.Inject

class MockRankingDataSource @Inject constructor() : RankingDataSource {

    override suspend fun getRankingItems(): Result<List<RankingItem>> {
        val items = listOf(
            ContentItem("r1", "EVA新世纪福音战士", "庵野秀明", "角川书店", 4.9f, listOf("机甲", "哲学", "经典")),
            ContentItem("r2", "86-不存在的战区", "安里アサト", "电击文库", 4.8f, listOf("科幻", "战争", "机甲")),
            ContentItem("r3", "Fate/Zero", "虚渊玄", "TYPE-MOON", 4.8f, listOf("圣杯战争", "黑暗", "史诗")),
            ContentItem("r4", "刀剑神域", "川原砾", "电击文库", 4.8f, listOf("科幻", "冒险", "虚拟现实")),
            ContentItem("r5", "狼与香辛料", "支仓冻砂", "电击文库", 4.7f, listOf("冒险", "经商", "奇幻")),
            ContentItem("r6", "Re:从零开始的异世界生活", "长月达平", "MF文库J", 4.7f, listOf("异世界", "悬疑", "轮回")),
            ContentItem("r7", "OVERLORD", "丸山くがね", "Enterbrain", 4.7f, listOf("异世界", "黑暗", "奇幻")),
            ContentItem("r8", "物语系列", "西尾维新", "讲谈社BOX", 4.6f, listOf("怪谈", "悬疑", "青春")),
            ContentItem("r9", "关于我转生变成史莱姆这档事", "伏濑", "GC Novels", 4.6f, listOf("异世界", "奇幻", "轻松")),
            ContentItem("r10", "青春猪头少年系列", "鸭志田一", "电击文库", 4.6f, listOf("恋爱", "青春", "校园")),
        )
        return Result.success(items.mapIndexed { index, item ->
            val trend = when (index) {
                0, 2, 5 -> RankTrend.UP
                1, 6, 8 -> RankTrend.DOWN
                else -> RankTrend.STABLE
            }
            RankingItem(rank = index + 1, item = item, trend = trend)
        })
    }
}
