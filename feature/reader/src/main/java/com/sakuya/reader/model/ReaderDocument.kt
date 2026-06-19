package com.sakuya.reader.model


sealed interface ReaderDocument{
    val title: String

    data class Txt(
        override val title: String,
        val text: String
    ) : ReaderDocument

    data class Epub(
        override val title: String,
        val chapters: List<ReaderChapter>
    ) : ReaderDocument
}

data class ReaderChapter(
    val index: Int,
    val title: String,
    val content: String
)