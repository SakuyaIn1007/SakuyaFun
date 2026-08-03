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

        builder.addMigrations(MIGRATION_5_6)

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
}
