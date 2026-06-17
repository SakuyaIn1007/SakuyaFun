package com.sakuya.reader.data

import android.content.Context

class ReaderPrefs(private val context: Context) {
    fun saveProgress(bookId: String, page: Int){
        context.getSharedPreferences("reader", 0)
            .edit()
            .putInt(bookId, page)
            .apply()
    }
}