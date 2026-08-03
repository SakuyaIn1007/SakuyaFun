package com.sakuya.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sakuya.data.local.entity.LibraryBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryBookDao {
    @Query("SELECT * FROM library_books ORDER BY collectedAt DESC")
    fun observeBooks(): Flow<List<LibraryBookEntity>>

    @Upsert
    suspend fun upsertBook(book: LibraryBookEntity)

    @Query("SELECT * FROM library_books WHERE id = :id LIMIT 1")
    suspend fun getBook(id: String): LibraryBookEntity?

    @Query("SELECT * FROM library_books WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun getBookByTitle(title: String): LibraryBookEntity?

    @Query("DELETE FROM library_books WHERE id = :id")
    suspend fun deleteBook(id: String)
}
