package com.sakuya.reader.model

sealed class BookSource {

    data class Local(
        val filePath: String
    ) : BookSource()

    data class Remote(
        val url: String
    ) : BookSource()
}

data class Book(
    val id: Int,
    val title: String,
    val source: BookSource
)