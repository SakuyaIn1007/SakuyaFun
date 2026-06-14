package com.sakuya.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sakuya.data.local.entity.ChatMessageEntity
import com.sakuya.data.local.entity.MessageType
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * from chat_messages where conversationId = :conversationId order by timeStamp desc limit :limit offset :offset")
    fun observeMessagePaged(conversationId: String, limit: Int, offset: Int): Flow<List<ChatMessageEntity>>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE from chat_messages where id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE from chat_messages where conversationId = :conversationId")
    suspend fun deleteMessageByConversation(conversationId: String)

    /*跨表接收消息的事务
    * 当App收到一条新聊天消息时，在底层其实需要同时执行两个动作：
    * 1. 在消息表追加一条消息记录
    * 2. 对应的会话表必须同步更新它的 lastMessage，timeLabel 以及未读数 unreadCount
    *
    * */
    @Transaction
    suspend fun handleIncomingMessage(message: ChatMessageEntity){
        insertMessage(message)

        val displaySummary = when (message.messageType){
            MessageType.TEXT -> message.content
            MessageType.IMAGE -> "[图文消息]"
            MessageType.FILE -> "[文件]"
        }

        updateConversationSummary(
            conversationId = message.conversationId,
            lastMsg = displaySummary,
            timeLabel = message.timeLabel,
            activeTime = message.timeStamp
        )
    }

    @Query("UPDATE conversations set lastMessage = :lastMsg, timeLabel = :timeLabel, lastActiveTime = :activeTime, unreadCount = unreadCount + 1 where id = :conversationId")
    suspend fun updateConversationSummary(conversationId: String, lastMsg: String, timeLabel: String, activeTime: Long)
}

