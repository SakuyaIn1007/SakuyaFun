package com.sakuya.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sakuya.data.local.entity.FeedCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * FeedCacheDao.kt
 * 职责说明：管理首页关注、推荐两个分流的 Room 离线缓存。
 * 执行流程：首刷事务清除一个分流后完整写入；分页写入按动态主键覆盖去重；页面始终观察本表。
 */
@Dao
interface FeedCacheDao {
    @Query("SELECT * FROM feed_cache WHERE stream = :stream ORDER BY position ASC")
    fun observeByStream(stream: String): Flow<List<FeedCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FeedCacheEntity>)

    @Query("DELETE FROM feed_cache WHERE stream = :stream")
    suspend fun clearStream(stream: String)

    /** 刷新必须原子替换，防止页面在删除旧缓存与插入新缓存之间观察到空列表。 */
    @Transaction
    suspend fun replaceStream(stream: String, items: List<FeedCacheEntity>) {
        clearStream(stream)
        upsertAll(items)
    }
}
