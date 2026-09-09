package com.sakuya.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.sakuya.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastActiveTime DESC")
    fun observeAll(): Flow<List<ConversationEntity>>
    /** 底部消息红点只关心是否存在未读，数据库求和避免 UI 扫描整表。 */
    @Query("SELECT COALESCE(SUM(unreadCount), 0) FROM conversations")
    fun observeTotalUnreadCount(): Flow<Int>

    @Query("SELECT * from conversations where id = :id")
    suspend fun getById(id: String): ConversationEntity?

//  update + insert 覆盖
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertAll(conversations: List<ConversationEntity>)
    @Delete
    suspend fun delete(conversation: ConversationEntity)

//  新增未读消息
    @Query("UPDATE conversations set unreadCount = unreadCount + 1 where id = :id")
    suspend fun incrementUnread(id: String)

    /** 详情页保存成功后局部更新缓存，列表无需等待下一次整表刷新。 */
    @Query("UPDATE conversations SET isPinned = :isPinned, isMuted = :isMuted WHERE id = :id")
    suspend fun updatePreferences(id: String, isPinned: Boolean, isMuted: Boolean)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}
