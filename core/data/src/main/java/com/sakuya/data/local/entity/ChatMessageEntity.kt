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
    val content: String,
    val timeLabel: String,
    val isMine: Boolean,
    val timeStamp: Long,
    val messageType: MessageType = MessageType.TEXT
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