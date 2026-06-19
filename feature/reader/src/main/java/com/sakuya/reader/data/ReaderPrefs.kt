package com.sakuya.reader.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ReaderPrefs @Inject constructor(
        @ApplicationContext private val context: Context
) {

    private val prefs = context.getSharedPreferences("reader", Context.MODE_PRIVATE)

    fun saveProgress(bookKey: String, progress: Float){
        prefs.edit()
            .putFloat(progressKey(bookKey), progress)
            .apply()
    }

    fun gerProgress(bookKey: String): Float {
        return prefs.getFloat(progressKey(bookKey), 0f)
    }

    fun saveFontSize(fontSizeSp: Float) {
        prefs.edit()
            .putFloat(FONT_SIZE_KEY, fontSizeSp)
            .apply()
    }

    fun getFontSize() : Float{
        return prefs.getFloat(FONT_SIZE_KEY, 18f)
    }

    private fun progressKey(bookKey: String): String {
        return "progress:$bookKey"
    }

    private companion object {
        const val FONT_SIZE_KEY = "font_size_sp"
    }
}