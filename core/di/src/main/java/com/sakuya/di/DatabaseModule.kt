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
import com.sakuya.data.local.dao.LibraryBookDao
import com.sakuya.data.local.dao.ReadingProgressDao
import com.sakuya.data.local.dao.UserDao
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

        builder.addMigrations(MIGRATION_5_6, MIGRATION_6_7)

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
}
