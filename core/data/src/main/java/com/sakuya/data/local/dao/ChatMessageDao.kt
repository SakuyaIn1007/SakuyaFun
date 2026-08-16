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
    /**
     * 聊天页面的本地数据源。
     * 执行流程：Room 数据变化后自动发射完整会话记录，UI 因而可先显示缓存，再显示网络同步或实时消息。
     */
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timeStamp ASC")
    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * from chat_messages where conversationId = :conversationId order by timeStamp desc limit :limit offset :offset")
    fun observeMessagePaged(conversationId: String, limit: Int, offset: Int): Flow<List<ChatMessageEntity>>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    /** 服务端同步和发送状态更新需要覆盖已有记录，避免消息 ID 重复造成旧状态残留。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE from chat_messages where id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE from chat_messages where conversationId = :conversationId")
    suspend fun deleteMessageByConversation(conversationId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()

    /**
     * 消息与会话摘要的跨表写入事务。
     * 执行流程：无论是本地 PENDING/FAILED、REST 发送确认还是 WebSocket 实时消息，均先写消息表，
     * 再用同一消息更新 conversations 的 lastMessage、timeLabel 与 lastActiveTime。
     * 对方消息才递增 unreadCount；isMine=true 的本地和已确认发送消息只更新摘要，防止误增未读。
     */
    @Transaction
    suspend fun handleIncomingMessage(message: ChatMessageEntity){
        upsertMessage(message)

        val displaySummary = when (message.messageType){
            MessageType.TEXT -> message.content
            MessageType.IMAGE -> "[图文消息]"
            MessageType.FILE -> "[文件]"
        }

        if (message.isMine) {
            updateConversationSummaryWithoutUnread(
                conversationId = message.conversationId,
                lastMsg = displaySummary,
                timeLabel = message.timeLabel,
                activeTime = message.timeStamp
            )
        } else {
            updateConversationSummary(
                conversationId = message.conversationId,
                lastMsg = displaySummary,
                timeLabel = message.timeLabel,
                activeTime = message.timeStamp
            )
        }
    }

    @Query("UPDATE conversations set lastMessage = :lastMsg, timeLabel = :timeLabel, lastActiveTime = :activeTime, unreadCount = unreadCount + 1 where id = :conversationId")
    suspend fun updateConversationSummary(conversationId: String, lastMsg: String, timeLabel: String, activeTime: Long)

    /** 自己发送的消息更新会话摘要，但不能增加未读数。 */
    @Query("UPDATE conversations SET lastMessage = :lastMsg, timeLabel = :timeLabel, lastActiveTime = :activeTime WHERE id = :conversationId")
    suspend fun updateConversationSummaryWithoutUnread(conversationId: String, lastMsg: String, timeLabel: String, activeTime: Long)

    /** 当前会话打开时，服务端标记已读成功后同步清除本地未读徽标。 */
    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markConversationAsRead(conversationId: String)
}
