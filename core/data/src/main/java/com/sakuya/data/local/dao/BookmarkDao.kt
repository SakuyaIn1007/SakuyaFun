package com.sakuya.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sakuya.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun observeBookmarks(bookId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    suspend fun getBookmarks(bookId: String): List<BookmarkEntity>

    @Upsert
    suspend fun upsertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteBookmarkByBookId(bookId: String)

    /** 内容源解耦后将旧 wenku8:* 书签归并到稳定 content:* 键。 */
    @Query("UPDATE bookmarks SET bookId = :newBookId WHERE bookId = :oldBookId")
    suspend fun moveBookmarks(oldBookId: String, newBookId: String)
}
