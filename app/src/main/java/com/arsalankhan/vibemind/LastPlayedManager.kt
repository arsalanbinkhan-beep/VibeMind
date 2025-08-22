package com.arsalankhan.vibemind

import android.content.Context
import android.content.Context.MODE_PRIVATE

object LastPlayedManager {
    private const val PREF_NAME = "LAST_PLAYED"

    fun saveLastPlayedSong(context: Context, song: Song) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonList = prefs.getString("songs", "[]")
        val songList = mutableListOf<Song>()

        try {
            val jsonArray = org.json.JSONArray(jsonList)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                songList.add(
                    Song(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        path = obj.getString("path"),
                        duration = obj.getLong("duration"),
                        albumId = obj.getLong("albumId")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Remove if already exists
        songList.removeAll { it.id == song.id }

        // Add to top
        songList.add(0, song)

        // Keep only 3
        if (songList.size > 3) {
            songList.subList(3, songList.size).clear()
        }

        // Save back
        val newJsonArray = org.json.JSONArray()
        songList.forEach {
            val obj = org.json.JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("artist", it.artist)
                put("path", it.path)
                put("duration", it.duration)
                put("albumId", it.albumId)
            }
            newJsonArray.put(obj)
        }

        prefs.edit().putString("songs", newJsonArray.toString()).apply()
    }

    fun getLastPlayedSongs(context: Context, allSongs: List<Song>): List<Song> {
        val prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val jsonList = prefs.getString("songs", "[]")
        val songList = mutableListOf<Song>()

        try {
            val jsonArray = org.json.JSONArray(jsonList)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val song = allSongs.find { it.id == obj.getLong("id") }
                if (song != null) {
                    songList.add(song)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return songList
    }
}