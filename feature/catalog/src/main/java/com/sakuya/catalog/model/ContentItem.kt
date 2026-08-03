package com.sakuya.catalog.model

data class ContentItem(
    val id: String,
    val title: String,
    val author: String,
    val publisher: String,
    val rating: Float,
    val tags: List<String>,
    // Older catalog responses did not include this field, so it must remain nullable at the API boundary.
    val description: String? = null,
    val isCollected: Boolean = false
) {
    val subtitle: String get() = "$author · $publisher"
}

data class RankingItem(
    val rank: Int,
    val item: ContentItem
)

data class BannerData(
    val title: String,
    val subtitle: String,
    val badge: String
)
