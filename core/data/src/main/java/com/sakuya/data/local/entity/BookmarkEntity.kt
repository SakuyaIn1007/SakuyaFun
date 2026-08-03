package com.sakuya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val title: String,
    val progress: Float,
    val note: String,
    val createdAt: Long
)
