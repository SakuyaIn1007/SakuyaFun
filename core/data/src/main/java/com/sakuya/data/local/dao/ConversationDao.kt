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

    @Query("SELECT * from conversations order by lastActiveTime desc")
    fun observeAll(): Flow<List<ConversationEntity>>

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

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}
