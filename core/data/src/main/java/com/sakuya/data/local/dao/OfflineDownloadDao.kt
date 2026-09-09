package com.sakuya.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sakuya.data.local.entity.OfflineChapterEntity
import com.sakuya.data.local.entity.OfflineDownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineDownloadDao {
    @Query("SELECT * FROM offline_downloads WHERE ownerId = :ownerId AND bookId = :bookId LIMIT 1")
    fun observe(ownerId: String, bookId: String): Flow<OfflineDownloadEntity?>

    @Query("SELECT * FROM offline_downloads WHERE ownerId = :ownerId AND bookId = :bookId LIMIT 1")
    suspend fun get(ownerId: String, bookId: String): OfflineDownloadEntity?

    @Query("SELECT * FROM offline_downloads WHERE ownerId = :ownerId AND status IN ('QUEUED', 'DOWNLOADING') ORDER BY updatedAt")
    suspend fun resumable(ownerId: String): List<OfflineDownloadEntity>

    @Query("SELECT * FROM offline_downloads WHERE ownerId = :ownerId ORDER BY updatedAt DESC")
    fun observeAll(ownerId: String): Flow<List<OfflineDownloadEntity>>

    @Query("SELECT * FROM offline_chapters WHERE ownerId = :ownerId AND bookId = :bookId ORDER BY chapterOrder")
    suspend fun chapters(ownerId: String, bookId: String): List<OfflineChapterEntity>

    @Upsert suspend fun upsertDownload(entity: OfflineDownloadEntity)
    @Upsert suspend fun upsertChapter(entity: OfflineChapterEntity)

    @Query("DELETE FROM offline_chapters WHERE ownerId = :ownerId AND bookId = :bookId")
    suspend fun deleteChapters(ownerId: String, bookId: String)

    @Query("DELETE FROM offline_downloads WHERE ownerId = :ownerId AND bookId = :bookId")
    suspend fun deleteDownload(ownerId: String, bookId: String)
}
