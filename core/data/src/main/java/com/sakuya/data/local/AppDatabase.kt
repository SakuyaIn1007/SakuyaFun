package com.sakuya.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sakuya.data.local.dao.BookmarkDao
import com.sakuya.data.local.dao.ChatMessageDao
import com.sakuya.data.local.dao.ConversationDao
import com.sakuya.data.local.dao.FeedCacheDao
import com.sakuya.data.local.dao.LibraryBookDao
import com.sakuya.data.local.dao.ReadingProgressDao
import com.sakuya.data.local.dao.UserDao
import com.sakuya.data.local.entity.BookmarkEntity
import com.sakuya.data.local.entity.ChatMessageEntity
import com.sakuya.data.local.entity.ConversationEntity
import com.sakuya.data.local.entity.FeedCacheEntity
import com.sakuya.data.local.entity.LibraryBookEntity
import com.sakuya.data.local.entity.ReadingProgressEntity
import com.sakuya.data.local.entity.UserEntity
import com.sakuya.data.local.entity.NotificationEntity
import com.sakuya.data.local.dao.NotificationDao
import com.sakuya.data.local.dao.OnlineBookmarkDao
import com.sakuya.data.local.dao.OnlineReadingProgressDao
import com.sakuya.data.local.entity.OnlineBookmarkEntity
import com.sakuya.data.local.entity.OnlineReadingProgressEntity
import com.sakuya.data.local.dao.OfflineDownloadDao
import com.sakuya.data.local.entity.OfflineChapterEntity
import com.sakuya.data.local.entity.OfflineDownloadEntity


@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class,
        UserEntity::class,
        LibraryBookEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        FeedCacheEntity::class,
        NotificationEntity::class,
        OnlineReadingProgressEntity::class,
        OnlineBookmarkEntity::class,
        OfflineDownloadEntity::class,
        OfflineChapterEntity::class,
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryBookDao(): LibraryBookDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun userDao(): UserDao
    abstract fun feedCacheDao(): FeedCacheDao
    abstract fun notificationDao(): NotificationDao
    abstract fun onlineReadingProgressDao(): OnlineReadingProgressDao
    abstract fun onlineBookmarkDao(): OnlineBookmarkDao
    abstract fun offlineDownloadDao(): OfflineDownloadDao
}
