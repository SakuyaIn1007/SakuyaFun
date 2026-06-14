package com.sakuya.library.viewmodel

import androidx.lifecycle.ViewModel
import com.sakuya.library.model.LibraryItem
import com.sakuya.library.model.LibraryItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor() : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _novelItems = MutableStateFlow(sampleNovelItems())
    val novelItems: StateFlow<List<LibraryItem>> = _novelItems.asStateFlow()

    private val _musicItems = MutableStateFlow(sampleMusicItems())
    val musicItems: StateFlow<List<LibraryItem>> = _musicItems.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun removeItem(id: String) {
        _novelItems.value = _novelItems.value.filter { it.id != id }
        _musicItems.value = _musicItems.value.filter { it.id != id }
    }
}

private fun sampleNovelItems() = listOf(
    LibraryItem(
        id = "novel_1",
        title = "刀剑神域",
        subtitle = "川原砾 · 电击文库",
        rating = 4.8f,
        tags = listOf("科幻", "冒险", "虚拟现实"),
        type = LibraryItemType.NOVEL,
        collectedAt = "2026-06-01"
    ),
    LibraryItem(
        id = "novel_2",
        title = "关于我转生变成史莱姆这档事",
        subtitle = "伏濑 · GC Novels",
        rating = 4.6f,
        tags = listOf("异世界", "奇幻", "轻松"),
        type = LibraryItemType.NOVEL,
        collectedAt = "2026-05-28"
    ),
    LibraryItem(
        id = "novel_3",
        title = "Re:从零开始的异世界生活",
        subtitle = "长月达平 · MF文库J",
        rating = 4.7f,
        tags = listOf("异世界", "悬疑", "轮回"),
        type = LibraryItemType.NOVEL,
        collectedAt = "2026-05-20"
    ),
    LibraryItem(
        id = "novel_4",
        title = "无职转生",
        subtitle = "理不尽な孫の手 · MF Books",
        rating = 4.5f,
        tags = listOf("异世界", "成长", "冒险"),
        type = LibraryItemType.NOVEL,
        collectedAt = "2026-05-15"
    ),
    LibraryItem(
        id = "novel_5",
        title = "86-不存在的战区",
        subtitle = "安里アサト · 电击文库",
        rating = 4.8f,
        tags = listOf("科幻", "战争", "机甲"),
        type = LibraryItemType.NOVEL,
        collectedAt = "2026-05-10"
    ),
    LibraryItem(
        id = "novel_6",
        title = "狼与香辛料",
        subtitle = "支仓冻砂 · 电击文库",
        rating = 4.7f,
        tags = listOf("冒险", "经商", "奇幻"),
        type = LibraryItemType.NOVEL,
        collectedAt = "2026-05-05"
    ),
    LibraryItem(
        id = "novel_7",
        title = "OVERLORD",
        subtitle = "丸山くがね · Enterbrain",
        rating = 4.7f,
        tags = listOf("异世界", "黑暗", "奇幻"),
        type = LibraryItemType.NOVEL,
        collectedAt = "2026-04-28"
    ),
    LibraryItem(
        id = "novel_8",
        title = "青春猪头少年系列",
        subtitle = "鸭志田一 · 电击文库",
        rating = 4.6f,
        tags = listOf("恋爱", "青春", "校园"),
        type = LibraryItemType.NOVEL,
        collectedAt = "2026-04-20"
    ),
)

private fun sampleMusicItems() = listOf(
    LibraryItem(
        id = "music_1",
        title = "Gurenge",
        subtitle = "LiSA · 鬼灭之刃 OP",
        rating = 4.9f,
        tags = listOf("动漫", "热血", "摇滚"),
        type = LibraryItemType.MUSIC,
        collectedAt = "2026-06-02"
    ),
    LibraryItem(
        id = "music_2",
        title = "IDOL",
        subtitle = "YOASOBI · 我推的孩子 OP",
        rating = 4.8f,
        tags = listOf("动漫", "流行", "电子"),
        type = LibraryItemType.MUSIC,
        collectedAt = "2026-05-30"
    ),
    LibraryItem(
        id = "music_3",
        title = "KICK BACK",
        subtitle = "米津玄师 · 电锯人 OP",
        rating = 4.7f,
        tags = listOf("动漫", "摇滚", "另类"),
        type = LibraryItemType.MUSIC,
        collectedAt = "2026-05-25"
    ),
    LibraryItem(
        id = "music_4",
        title = "紅蓮華",
        subtitle = "LiSA · 鬼灭之刃 OP",
        rating = 4.9f,
        tags = listOf("动漫", "热血", "摇滚"),
        type = LibraryItemType.MUSIC,
        collectedAt = "2026-05-18"
    ),
    LibraryItem(
        id = "music_5",
        title = "夜に駆ける",
        subtitle = "YOASOBI · 单曲",
        rating = 4.8f,
        tags = listOf("流行", "电子", "节奏"),
        type = LibraryItemType.MUSIC,
        collectedAt = "2026-05-12"
    ),
    LibraryItem(
        id = "music_6",
        title = "廻廻奇譚",
        subtitle = "Eve · 咒术回战 OP",
        rating = 4.7f,
        tags = listOf("动漫", "摇滚", "J-Rock"),
        type = LibraryItemType.MUSIC,
        collectedAt = "2026-05-08"
    ),
)
