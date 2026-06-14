package com.sakuya.home.data.datasource

import com.sakuya.home.model.ContentItem
import com.sakuya.home.model.RankTrend
import com.sakuya.home.model.RankingItem
import javax.inject.Inject

class MockHomeDataSource @Inject constructor() : HomeDataSource {

    override suspend fun getRecommendItems(): Result<List<ContentItem>> {
        return Result.success(listOf(
            ContentItem("1", "刀剑神域", "川原砾", "电击文库", 4.8f, listOf("科幻", "冒险", "虚拟现实")),
            ContentItem("2", "关于我转生变成史莱姆这档事", "伏濑", "GC Novels", 4.6f, listOf("异世界", "奇幻", "轻松")),
            ContentItem("3", "Re:从零开始的异世界生活", "长月达平", "MF文库J", 4.7f, listOf("异世界", "悬疑", "轮回")),
            ContentItem("4", "无职转生", "理不尽な孫の手", "MF Books", 4.5f, listOf("异世界", "成长", "冒险")),
            ContentItem("5", "盾之勇者成名录", "アネコユサギ", "MF Books", 4.3f, listOf("异世界", "复仇", "冒险")),
            ContentItem("6", "为美好的世界献上祝福", "暁なつめ", "角川Sneaker", 4.4f, listOf("异世界", "搞笑", "冒险")),
            ContentItem("7", "OVERLORD", "丸山くがね", "Enterbrain", 4.7f, listOf("异世界", "黑暗", "奇幻")),
            ContentItem("8", "青春猪头少年系列", "鸭志田一", "电击文库", 4.6f, listOf("恋爱", "青春", "校园")),
            ContentItem("9", "欢迎来到实力至上主义的教室", "衣笠彰梧", "MF文库J", 4.5f, listOf("校园", "智斗", "悬疑")),
            ContentItem("10", "吹响吧！上低音号", "武田绫乃", "宝岛社", 4.4f, listOf("音乐", "青春", "校园")),
        ))
    }

    override suspend fun getNovelItems(): Result<List<ContentItem>> {
        return Result.success(listOf(
            ContentItem("n1", "86-不存在的战区", "安里アサト", "电击文库", 4.8f, listOf("科幻", "战争", "机甲")),
            ContentItem("n2", "魔法科高校的劣等生", "佐岛勤", "电击文库", 4.4f, listOf("科幻", "校园", "魔法")),
            ContentItem("n3", "古书堂事件手帖", "三上延", "MediaWorks", 4.3f, listOf("悬疑", "治愈", "日常")),
            ContentItem("n4", "狼与香辛料", "支仓冻砂", "电击文库", 4.7f, listOf("冒险", "经商", "奇幻")),
            ContentItem("n5", "物语系列", "西尾维新", "讲谈社BOX", 4.6f, listOf("怪谈", "悬疑", "青春")),
            ContentItem("n6", "NO GAME NO LIFE", "榎宫祐", "MF文库J", 4.5f, listOf("异世界", "智斗", "奇幻")),
            ContentItem("n7", "文豪野犬", "朝雾卡夫卡", "角川Beans", 4.3f, listOf("超能力", "悬疑", "文学")),
            ContentItem("n8", "地错-在地下城寻求邂逅", "大森藤野", "GA文库", 4.5f, listOf("冒险", "奇幻", "恋爱")),
            ContentItem("n9", "Fate/Zero", "虚渊玄", "TYPE-MOON", 4.8f, listOf("圣杯战争", "黑暗", "史诗")),
            ContentItem("n10", "樱花庄的宠物女孩", "鸭志田一", "电击文库", 4.4f, listOf("恋爱", "青春", "校园")),
        ))
    }
}
