package com.sakuya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val bookKey: String,
    val progress: Float,
    val updatedAt: Long
)
