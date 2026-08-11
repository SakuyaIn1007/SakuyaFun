package com.sakuya.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sakuya.data.local.dao.BookmarkDao
import com.sakuya.data.local.dao.ChatMessageDao
import com.sakuya.data.local.dao.ConversationDao
import com.sakuya.data.local.dao.LibraryBookDao
import com.sakuya.data.local.dao.ReadingProgressDao
import com.sakuya.data.local.dao.UserDao
import com.sakuya.data.local.entity.BookmarkEntity
import com.sakuya.data.local.entity.ChatMessageEntity
import com.sakuya.data.local.entity.ConversationEntity
import com.sakuya.data.local.entity.LibraryBookEntity
import com.sakuya.data.local.entity.ReadingProgressEntity
import com.sakuya.data.local.entity.UserEntity


@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class,
        UserEntity::class,
        LibraryBookEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryBookDao(): LibraryBookDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun userDao(): UserDao
}
