package com.sakuya.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sakuya.data.local.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress")
    fun observeProgress(): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress ORDER BY updatedAt DESC")
    fun observeRecentProgress(): Flow<List<ReadingProgressEntity>>
    @Query("SELECT * FROM reading_progress WHERE bookKey = :bookKey LIMIT 1")
    suspend fun getProgress(bookKey: String): ReadingProgressEntity?

    @Upsert
    suspend fun upsertProgress(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE bookKey = :bookKey")
    suspend fun deleteProgress(bookKey: String)
}
