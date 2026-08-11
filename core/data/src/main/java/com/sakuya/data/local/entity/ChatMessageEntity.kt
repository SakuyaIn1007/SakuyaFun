package com.sakuya.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE //级联删除:会话删了，消息也删.
        )
    ],
    indices = [Index(value = ["conversationId"])]
    )
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String = "",
    val content: String,
    val timeLabel: String,
    val isMine: Boolean,
    val avatarText: String = "",
    val timeStamp: Long,
    val messageType: MessageType = MessageType.TEXT,
    /** JSON 形式保存附件，避免聊天模块的领域模型反向依赖 core:data。 */
    val attachmentsJson: String = "[]",
    /** JSON 形式保存回复引用；无回复时为 null。 */
    val replyJson: String? = null,
    val sendStatus: String = "sent",
    val readStatus: String = "unread",
)

enum class MessageType(val typeName: String){
    TEXT("text"),
    IMAGE("image"),
    FILE("file");
    companion object {
        fun fromString(type: String?): MessageType =
            entries.firstOrNull { it.typeName.equals(type, ignoreCase = true) } ?: TEXT
    }
}
