package com.sakuya.home.model

data class ContentItem(
    val id: String,
    val title: String,
    val author: String,
    val publisher: String,
    val rating: Float,
    val tags: List<String>,
    val isCollected: Boolean = false
) {
    val subtitle: String get() = "$author · $publisher"
}

data class RankingItem(
    val rank: Int,
    val item: ContentItem,
    val trend: RankTrend = RankTrend.STABLE
)

enum class RankTrend { UP, DOWN, STABLE }

data class BannerData(
    val title: String,
    val subtitle: String,
    val badge: String
)
