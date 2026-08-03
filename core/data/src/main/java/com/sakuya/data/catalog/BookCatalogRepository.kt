package com.sakuya.data.catalog

import javax.inject.Inject
import javax.inject.Singleton

data class CatalogBook(
    val id: String,
    val title: String,
    val author: String,
    val publisher: String,
    val rating: Float,
    val tags: List<String>,
    val description: String
) {
    val subtitle: String get() = "$author · $publisher"
}

@Singleton
class BookCatalogRepository @Inject constructor() {
    fun getRecommendedBooks(): List<CatalogBook> = recommendedIds.mapNotNull(::findById)

    fun getNovelBooks(): List<CatalogBook> = novelIds.mapNotNull(::findById)

    fun getRankingBooks(): List<CatalogBook> = rankingIds.mapNotNull(::findById)

    fun findById(id: String): CatalogBook? = booksById[id]

    private companion object {
        val books = listOf(
            CatalogBook("sword-art-online", "刀剑神域", "川原砾", "电击文库", 4.8f, listOf("科幻", "冒险", "虚拟现实"), "以完全潜行游戏为舞台，讲述玩家在虚拟世界中求生与成长的冒险故事。"),
            CatalogBook("tensei-slime", "关于我转生变成史莱姆这档事", "伏濑", "GC Novels", 4.6f, listOf("异世界", "奇幻", "轻松"), "转生为史莱姆后建立伙伴与国家的异世界冒险。"),
            CatalogBook("rezero", "Re:从零开始的异世界生活", "长月达平", "MF文库J", 4.7f, listOf("异世界", "悬疑", "轮回"), "拥有死亡回归能力的少年，在反复轮回中守护重要之人。"),
            CatalogBook("mushoku-tensei", "无职转生", "理不尽な孫の手", "MF Books", 4.5f, listOf("异世界", "成长", "冒险"), "获得第二次人生的主人公，从幼年开始重新学习、成长与冒险。"),
            CatalogBook("shield-hero", "盾之勇者成名录", "アネコユサギ", "MF Books", 4.3f, listOf("异世界", "复仇", "冒险"), "被召唤为盾之勇者的青年，在误解与困境中重建信任。"),
            CatalogBook("konosuba", "为美好的世界献上祝福", "暁なつめ", "角川Sneaker", 4.4f, listOf("异世界", "搞笑", "冒险"), "一支能力出众却麻烦不断的小队展开的轻松异世界冒险。"),
            CatalogBook("overlord", "OVERLORD", "丸山くがね", "Enterbrain", 4.7f, listOf("异世界", "黑暗", "奇幻"), "游戏停服后仍留在异世界的统治者，开始探索未知世界。"),
            CatalogBook("bunny-girl-senpai", "青春猪头少年系列", "鸭志田一", "电击文库", 4.6f, listOf("恋爱", "青春", "校园"), "少年少女面对青春期综合征以及各自内心困境的校园故事。"),
            CatalogBook("classroom-elite", "欢迎来到实力至上主义的教室", "衣笠彰梧", "MF文库J", 4.5f, listOf("校园", "智斗", "悬疑"), "在实力至上的校园制度中，学生们围绕班级排名展开较量。"),
            CatalogBook("sound-euphonium", "吹响吧！上低音号", "武田绫乃", "宝岛社", 4.4f, listOf("音乐", "青春", "校园"), "高中吹奏乐部的成员以全国大赛为目标共同前进。"),
            CatalogBook("86", "86-不存在的战区", "安里アサト", "电击文库", 4.8f, listOf("科幻", "战争", "机甲"), "被隔离的少年少女驾驶无人兵器，在战场与偏见中寻找未来。"),
            CatalogBook("mahouka", "魔法科高校的劣等生", "佐岛勤", "电击文库", 4.4f, listOf("科幻", "校园", "魔法"), "魔法成为技术的时代，一对兄妹进入魔法高中后的故事。"),
            CatalogBook("biblia", "古书堂事件手帖", "三上延", "MediaWorks", 4.3f, listOf("悬疑", "治愈", "日常"), "古书店店主通过书籍与线索解开旧书背后隐藏的故事。"),
            CatalogBook("spice-and-wolf", "狼与香辛料", "支仓冻砂", "电击文库", 4.7f, listOf("冒险", "经商", "奇幻"), "旅行商人与狼神少女结伴远行，在贸易与旅途中彼此了解。"),
            CatalogBook("monogatari", "物语系列", "西尾维新", "讲谈社BOX", 4.6f, listOf("怪谈", "悬疑", "青春"), "少年与遭遇怪异的少女们相遇，并直面各自内心的问题。"),
            CatalogBook("no-game-no-life", "NO GAME NO LIFE", "榎宫祐", "MF文库J", 4.5f, listOf("异世界", "智斗", "奇幻"), "天才游戏玩家兄妹来到一切由游戏决定的世界。"),
            CatalogBook("bungo-stray-dogs", "文豪野犬", "朝雾卡夫卡", "角川Beans", 4.3f, listOf("超能力", "悬疑", "文学"), "拥有文学家之名与异能力的人们卷入城市中的多方纷争。"),
            CatalogBook("danmachi", "地错-在地下城寻求邂逅", "大森藤野", "GA文库", 4.5f, listOf("冒险", "奇幻", "恋爱"), "新手冒险者在迷宫都市中邂逅伙伴并不断成长。"),
            CatalogBook("fate-zero", "Fate/Zero", "虚渊玄", "TYPE-MOON", 4.8f, listOf("圣杯战争", "黑暗", "史诗"), "七位魔术师与英灵围绕圣杯展开残酷战争。"),
            CatalogBook("sakurasou", "樱花庄的宠物女孩", "鸭志田一", "电击文库", 4.4f, listOf("恋爱", "青春", "校园"), "住在樱花庄的少年少女，在创作、才能与青春中寻找方向。"),
            CatalogBook("evangelion", "EVA新世纪福音战士", "庵野秀明", "角川书店", 4.9f, listOf("机甲", "哲学", "经典"), "少年驾驶巨大人形兵器迎战使徒，也逐渐面对自我与他人的边界。")
        )
        val booksById = books.associateBy(CatalogBook::id)
        val recommendedIds = listOf("sword-art-online", "tensei-slime", "rezero", "mushoku-tensei", "shield-hero", "konosuba", "overlord", "bunny-girl-senpai", "classroom-elite", "sound-euphonium")
        val novelIds = listOf("86", "mahouka", "biblia", "spice-and-wolf", "monogatari", "no-game-no-life", "bungo-stray-dogs", "danmachi", "fate-zero", "sakurasou")
        val rankingIds = listOf("evangelion", "86", "fate-zero", "sword-art-online", "spice-and-wolf", "rezero", "overlord", "monogatari", "tensei-slime", "bunny-girl-senpai")
    }
}
