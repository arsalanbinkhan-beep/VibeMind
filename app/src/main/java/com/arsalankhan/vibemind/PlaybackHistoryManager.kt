package com.arsalankhan.vibemind

import android.content.Context
import android.content.SharedPreferences

object PlaybackHistoryManager {
    private const val PREF_NAME = "play_history"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun incrementPlayCount(context: Context, songPath: String) {
        val prefs = getPrefs(context)
        val currentCount = prefs.getInt(songPath, 0)
        prefs.edit().putInt(songPath, currentCount + 1).apply()
    }

    fun getTopPlayedSongs(context: Context, allSongs: List<Song>, limit: Int = 10): List<Song> {
        val prefs = getPrefs(context)
        return allSongs
            .filter { prefs.contains(it.path) }
            .sortedByDescending { prefs.getInt(it.path, 0) }
            .take(limit)
    }
}
