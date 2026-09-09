package com.sakuya.data.local.dao

import androidx.room.*
import com.sakuya.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/** 通知列表始终观察 Room；远端刷新只更新缓存，不直接驱动 Compose。 */
@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC") fun observeAll(): Flow<List<NotificationEntity>>
    @Query("SELECT COUNT(*) FROM notifications WHERE readAt IS NULL") fun observeUnreadCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM notifications WHERE readAt IS NULL AND type IN (:types)") fun observeUnreadCount(types: List<String>): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(items: List<NotificationEntity>)
    @Query("UPDATE notifications SET readAt = :readAt WHERE id = :id") suspend fun markRead(id: String, readAt: String)
    @Query("UPDATE notifications SET readAt = :readAt WHERE readAt IS NULL") suspend fun markAllRead(readAt: String)
    @Query("UPDATE notifications SET readAt = :readAt WHERE readAt IS NULL AND type IN (:types)") suspend fun markTypesRead(types: List<String>,readAt: String)
    @Query("DELETE FROM notifications") suspend fun clearAll()
}
