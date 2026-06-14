package com.sakuya.library.model

data class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val rating: Float,
    val tags: List<String>,
    val type: LibraryItemType,
    val collectedAt: String
)

enum class LibraryItemType {
    NOVEL,
    MUSIC
}
