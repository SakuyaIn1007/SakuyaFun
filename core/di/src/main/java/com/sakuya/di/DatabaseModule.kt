package com.sakuya.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sakuya.data.BuildConfig
import com.sakuya.data.local.AppDatabase
import com.sakuya.data.local.dao.BookmarkDao
import com.sakuya.data.local.dao.ChatMessageDao
import com.sakuya.data.local.dao.ConversationDao
import com.sakuya.data.local.dao.FeedCacheDao
import com.sakuya.data.local.dao.LibraryBookDao
import com.sakuya.data.local.dao.ReadingProgressDao
import com.sakuya.data.local.dao.UserDao
import com.sakuya.data.local.dao.NotificationDao
import com.sakuya.data.local.dao.OnlineBookmarkDao
import com.sakuya.data.local.dao.OnlineReadingProgressDao
import com.sakuya.data.local.dao.OfflineDownloadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        val builder: RoomDatabase.Builder<AppDatabase> = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sakuya.db"
        )

        builder.addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)

        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration()
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideConversationDao(
        database: AppDatabase
    ): ConversationDao = database.conversationDao()

    @Provides
    @Singleton
    fun provideChatMessageDao(
        database: AppDatabase
    ): ChatMessageDao = database.chatMessageDao()

    @Provides
    @Singleton
    fun provideUserDao(
        database: AppDatabase
    ): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideLibraryBookDao(
        database: AppDatabase
    ): LibraryBookDao = database.libraryBookDao()

    @Provides
    @Singleton
    fun provideReadingProgressDao(
        database: AppDatabase
    ): ReadingProgressDao = database.readingProgressDao()

    @Provides
    @Singleton
    fun provideBookmarkDao(
        database: AppDatabase
    ): BookmarkDao = database.bookmarkDao()

    @Provides
    @Singleton
    fun provideFeedCacheDao(database: AppDatabase): FeedCacheDao = database.feedCacheDao()

    @Provides fun provideNotificationDao(database: AppDatabase): NotificationDao = database.notificationDao()

    @Provides fun provideOnlineReadingProgressDao(database: AppDatabase): OnlineReadingProgressDao = database.onlineReadingProgressDao()

    @Provides fun provideOnlineBookmarkDao(database: AppDatabase): OnlineBookmarkDao = database.onlineBookmarkDao()

    @Provides fun provideOfflineDownloadDao(database: AppDatabase): OfflineDownloadDao = database.offlineDownloadDao()

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user ADD COLUMN birthday TEXT")
            db.execSQL("ALTER TABLE user ADD COLUMN regionCode TEXT")
            db.execSQL("ALTER TABLE user ADD COLUMN pokeText TEXT")
            db.execSQL("ALTER TABLE user ADD COLUMN ringtoneName TEXT")
            db.execSQL("ALTER TABLE user ADD COLUMN canBeAddedByStrangers INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE user ADD COLUMN showProfileToStrangers INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE user ADD COLUMN muteMessagesFromUnknown INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * 聊天记录升级为可恢复的本地状态。
     * 执行流程：为已有消息补齐发送者、头像和发送/已读状态，旧记录以安全默认值继续可读。
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE chat_messages ADD COLUMN senderId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE chat_messages ADD COLUMN avatarText TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE chat_messages ADD COLUMN attachmentsJson TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("ALTER TABLE chat_messages ADD COLUMN replyJson TEXT")
            db.execSQL("ALTER TABLE chat_messages ADD COLUMN sendStatus TEXT NOT NULL DEFAULT 'sent'")
            db.execSQL("ALTER TABLE chat_messages ADD COLUMN readStatus TEXT NOT NULL DEFAULT 'unread'")
        }
    }

    /** 首页动态缓存表不改动既有业务数据，升级后首次成功同步才开始写入。 */
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `feed_cache` (`stream` TEXT NOT NULL, `postId` TEXT NOT NULL, `position` INTEGER NOT NULL, `userId` TEXT NOT NULL, `authorName` TEXT NOT NULL, `authorInitial` TEXT NOT NULL, `authorColor` INTEGER NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `attachmentsJson` TEXT NOT NULL, `imageColorsJson` TEXT NOT NULL, `tagsJson` TEXT NOT NULL, `publishedAt` TEXT NOT NULL, `relatedNovelJson` TEXT, `commentCount` INTEGER NOT NULL, `likeCount` INTEGER NOT NULL, `favoriteCount` INTEGER NOT NULL, `isLiked` INTEGER NOT NULL, `isFavorited` INTEGER NOT NULL, `isMine` INTEGER NOT NULL, PRIMARY KEY(`stream`, `postId`))")
        }
    }

    /** 会话偏好从服务端同步到本地，免打扰字段不影响已有会话与消息数据。 */
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE conversations ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * 阅读器从单一全局比例升级为章节锚点加章节内比例。
     * 旧记录保留 progress，并以默认章节位置平滑回退，不会丢失已有阅读历史或书签。
     */
    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE reading_progress ADD COLUMN contentType TEXT NOT NULL DEFAULT 'TXT'")
            db.execSQL("ALTER TABLE reading_progress ADD COLUMN chapterId TEXT")
            db.execSQL("ALTER TABLE reading_progress ADD COLUMN chapterIndex INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE reading_progress ADD COLUMN chapterProgress REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE bookmarks ADD COLUMN chapterId TEXT")
            db.execSQL("ALTER TABLE bookmarks ADD COLUMN chapterProgress REAL NOT NULL DEFAULT 0")
        }
    }

    /** 新增通知离线缓存，不修改任何既有业务表和用户数据。 */
    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `notifications` (`id` TEXT NOT NULL, `actorId` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `targetType` TEXT NOT NULL, `targetId` TEXT NOT NULL, `targetUserId` TEXT, `createdAt` TEXT NOT NULL, `readAt` TEXT, PRIMARY KEY(`id`))")
        }
    }

    /** 在线阅读状态按账号独立建表，原有本地 TXT/EPUB 进度与书签不做结构重写。 */
    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `online_reading_progress` (`ownerId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `contentType` TEXT NOT NULL, `progress` REAL NOT NULL, `chapterId` TEXT, `chapterIndex` INTEGER NOT NULL, `chapterProgress` REAL NOT NULL, `modifiedAt` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `deleted` INTEGER NOT NULL, `syncState` TEXT NOT NULL, PRIMARY KEY(`ownerId`, `bookId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `online_bookmarks` (`ownerId` TEXT NOT NULL, `id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `title` TEXT NOT NULL, `progress` REAL NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `chapterId` TEXT, `chapterProgress` REAL NOT NULL, `modifiedAt` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `deleted` INTEGER NOT NULL, `syncState` TEXT NOT NULL, PRIMARY KEY(`ownerId`, `id`))")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_online_bookmarks_ownerId_bookId` ON `online_bookmarks` (`ownerId`, `bookId`)")
        }
    }

    /** 新增按账号隔离的离线下载队列和章节文件索引，既有阅读状态不变。 */
    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `offline_downloads` (`ownerId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `title` TEXT NOT NULL, `status` TEXT NOT NULL, `completedChapters` INTEGER NOT NULL, `totalChapters` INTEGER NOT NULL, `downloadedBytes` INTEGER NOT NULL, `errorMessage` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`ownerId`, `bookId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `offline_chapters` (`ownerId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `chapterId` TEXT NOT NULL, `title` TEXT NOT NULL, `volumeTitle` TEXT NOT NULL, `chapterOrder` INTEGER NOT NULL, `filePath` TEXT NOT NULL, `byteSize` INTEGER NOT NULL, PRIMARY KEY(`ownerId`, `bookId`, `chapterId`))")
        }
    }
}
