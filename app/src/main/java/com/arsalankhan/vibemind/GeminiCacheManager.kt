package com.arsalankhan.vibemind

import android.content.Context

object GeminiCacheManager {
    private const val PREF_NAME = "gemini_cache"
    private const val KEY_CATEGORIZED_SONGS = "categorized_songs"
    private const val KEY_TIMESTAMP = "last_fetch_timestamp"
    private const val CACHE_DURATION = 7 * 24 * 60 * 60 * 1000L // 7 days cache

    fun saveCategorizedSongs(context: Context, songsJson: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CATEGORIZED_SONGS, songsJson)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getCachedSongs(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()

        // Check if cache is still valid (7 days)
        if (currentTime - timestamp > CACHE_DURATION) {
            return null // Cache expired
        }

        return prefs.getString(KEY_CATEGORIZED_SONGS, null)
    }

    fun clearCache(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun isCacheValid(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)
        return System.currentTimeMillis() - timestamp <= CACHE_DURATION
    }
}