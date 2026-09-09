package com.sakuya.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sakuya.data.local.entity.OnlineReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OnlineReadingProgressDao {
    @Query("SELECT * FROM online_reading_progress WHERE ownerId = :ownerId AND deleted = 0 ORDER BY modifiedAt DESC")
    fun observeActive(ownerId: String): Flow<List<OnlineReadingProgressEntity>>

    @Query("SELECT * FROM online_reading_progress WHERE ownerId = :ownerId AND bookId = :bookId LIMIT 1")
    suspend fun get(ownerId: String, bookId: String): OnlineReadingProgressEntity?

    @Query("SELECT * FROM online_reading_progress WHERE ownerId = :ownerId")
    suspend fun getAll(ownerId: String): List<OnlineReadingProgressEntity>

    @Query("SELECT * FROM online_reading_progress WHERE ownerId = :ownerId AND syncState != 'SYNCED'")
    suspend fun getPending(ownerId: String): List<OnlineReadingProgressEntity>

    @Upsert suspend fun upsert(entity: OnlineReadingProgressEntity)
    @Upsert suspend fun upsertAll(entities: List<OnlineReadingProgressEntity>)

    @Query("DELETE FROM online_reading_progress WHERE ownerId = :ownerId")
    suspend fun deleteOwner(ownerId: String)
}
