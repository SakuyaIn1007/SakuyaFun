package com.sakuya.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sakuya.data.local.dao.ChatMessageDao
import com.sakuya.data.local.dao.ConversationDao
import com.sakuya.data.local.dao.UserDao
import com.sakuya.data.local.entity.ChatMessageEntity
import com.sakuya.data.local.entity.ConversationEntity
import com.sakuya.data.local.entity.UserEntity


@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase(){
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun userDao(): UserDao
}