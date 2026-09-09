package com.sakuya.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sakuya.data.local.entity.OnlineBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OnlineBookmarkDao {
    @Query("SELECT * FROM online_bookmarks WHERE ownerId = :ownerId AND bookId = :bookId AND deleted = 0 ORDER BY createdAt DESC")
    fun observeActive(ownerId: String, bookId: String): Flow<List<OnlineBookmarkEntity>>

    @Query("SELECT * FROM online_bookmarks WHERE ownerId = :ownerId AND id = :id LIMIT 1")
    suspend fun get(ownerId: String, id: String): OnlineBookmarkEntity?

    @Query("SELECT * FROM online_bookmarks WHERE ownerId = :ownerId")
    suspend fun getAll(ownerId: String): List<OnlineBookmarkEntity>

    @Query("SELECT * FROM online_bookmarks WHERE ownerId = :ownerId AND syncState != 'SYNCED'")
    suspend fun getPending(ownerId: String): List<OnlineBookmarkEntity>

    @Query("SELECT * FROM online_bookmarks WHERE ownerId = :ownerId AND bookId = :bookId")
    suspend fun getByBook(ownerId: String, bookId: String): List<OnlineBookmarkEntity>

    @Upsert suspend fun upsert(entity: OnlineBookmarkEntity)
    @Upsert suspend fun upsertAll(entities: List<OnlineBookmarkEntity>)

    @Query("DELETE FROM online_bookmarks WHERE ownerId = :ownerId")
    suspend fun deleteOwner(ownerId: String)
}
