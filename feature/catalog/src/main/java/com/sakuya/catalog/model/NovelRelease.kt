package com.sakuya.catalog.model

/**
 * NovelRelease.kt
 * 职责说明：定义时间表页使用的已公布小说更新资料与日期分组模型。
 * 执行流程：ScheduleRepository 提供已公布更新 -> ViewModel 按日期分组并挑选推荐项 -> UI 按日期渲染小说条目。
 */
data class NovelRelease(
    val novel: ContentItem,
    val releaseDate: NovelReleaseDate,
    val volumeName: String,
    val isRecommended: Boolean = false,
)

/** 使用明确的年月日字段，兼容最低 Android 24，不依赖 java.time。 */
data class NovelReleaseDate(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<NovelReleaseDate> {
    override fun compareTo(other: NovelReleaseDate): Int =
        compareValuesBy(this, other, NovelReleaseDate::year, NovelReleaseDate::month, NovelReleaseDate::day)
}

data class NovelReleaseDayGroup(
    val date: NovelReleaseDate,
    val releases: List<NovelRelease>,
)
