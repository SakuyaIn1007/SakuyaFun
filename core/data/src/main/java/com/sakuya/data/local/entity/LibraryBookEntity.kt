package com.sakuya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_books")
data class LibraryBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val filePath: String,
    val rating: Float = 0f,
    val tags: String,
    val type: String,
    val collectedAt:Long,
    val updatedAt: Long
)