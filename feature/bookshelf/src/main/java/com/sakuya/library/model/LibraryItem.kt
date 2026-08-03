package com.sakuya.library.model

data class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val rating: Float,
    val tags: List<String>,
    val type: LibraryItemType,
    val collectedAt: String,
    val filePath: String = "",
    val progress: Float = 0f

){
    fun isValid(): Boolean = id.isNotBlank() && filePath.isNotBlank()
}

enum class LibraryItemType {
    TXT,
    EPUB
}
