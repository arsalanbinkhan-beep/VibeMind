package com.arsalankhan.vibemind

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

// PlaylistManager.kt
object PlaylistManager {
    private const val PREF_NAME = "playlists"
    private const val LIKED_PLAYLIST_ID = -1L
    private const val MOST_PLAYED_PLAYLIST_ID = -2L
    private const val FOR_YOU_PLAYLIST_ID = -3L

    private lateinit var prefs: SharedPreferences
    private var playlists = mutableListOf<Playlist>()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadPlaylists(context)
    }

    private fun loadPlaylists(context: Context) {
        // Create default playlists if they don't exist
        if (playlists.none { it.id == LIKED_PLAYLIST_ID }) {
            playlists.add(Playlist(LIKED_PLAYLIST_ID, "Liked Songs", mutableListOf()))
        }
        if (playlists.none { it.id == MOST_PLAYED_PLAYLIST_ID }) {
            playlists.add(Playlist(MOST_PLAYED_PLAYLIST_ID, "Most Played", mutableListOf()))
        }
        if (playlists.none { it.id == FOR_YOU_PLAYLIST_ID }) {
            playlists.add(Playlist(FOR_YOU_PLAYLIST_ID, "For You", mutableListOf()))
        }

        // Load user-created playlists from SharedPreferences
        val savedPlaylists = prefs.getStringSet("user_playlists", setOf()) ?: setOf()
        savedPlaylists.forEach { json ->
            try {
                val playlist = parsePlaylistJson(json)
                playlists.add(playlist)
            } catch (e: Exception) {
                Log.e("PlaylistManager", "Error parsing playlist: ${e.message}")
            }
        }
    }



        fun getForYouPlaylist(context: Context, allSongs: List<Song>): Playlist {
            val forYou = playlists.first { it.id == FOR_YOU_PLAYLIST_ID }
            forYou.songs.clear()
            forYou.songs.addAll(allSongs.shuffled().take(10))
            forYou.name = "For You"
            forYou.artist = "Based on your listening"
            return forYou
        }

        fun getMostPlayedPlaylist(context: Context, allSongs: List<Song>): Playlist {
            val mostPlayed = playlists.first { it.id == MOST_PLAYED_PLAYLIST_ID }
            mostPlayed.songs.clear()
            mostPlayed.songs.addAll(PlaybackHistoryManager.getTopPlayedSongs(context, allSongs, 10))
            mostPlayed.name = "Most Played"
            mostPlayed.artist = "Your top tracks"
            return mostPlayed
        }

        fun getLikedSongsPlaylist(): Playlist {
            val liked = playlists.first { it.id == LIKED_PLAYLIST_ID }
            liked.name = "Liked Songs"
            liked.artist = "Your favorite tracks"
            return liked
        }

        // ... rest of the existing code ...

    fun getUserPlaylists(): List<Playlist> {
        return playlists.filter { it.isUserCreated }
    }

    fun createNewPlaylist(name: String, initialSongs: List<Song> = emptyList()): Playlist {
        val newId = System.currentTimeMillis() // Simple ID generation
        val newPlaylist = Playlist(newId, name, initialSongs.toMutableList(), "You",true)
        playlists.add(newPlaylist)
        saveUserPlaylists()
        return newPlaylist
    }

    fun addToLikedSongs(song: Song) {
        val likedPlaylist = getLikedSongsPlaylist()
        if (!likedPlaylist.songs.any { it.id == song.id }) {
            song.isLiked = true
            likedPlaylist.songs.add(song)
        }
    }

    fun removeFromLikedSongs(song: Song) {
        val likedPlaylist = getLikedSongsPlaylist()
        likedPlaylist.songs.removeAll { it.id == song.id }
        song.isLiked = false
    }

    fun saveUserPlaylists() {
        val userPlaylists = playlists.filter { it.isUserCreated }
        val jsonSet = userPlaylists.map { playlistToJson(it) }.toSet()
        prefs.edit().putStringSet("user_playlists", jsonSet).apply()
    }

    private fun playlistToJson(playlist: Playlist): String {
        return JSONObject().apply {
            put("id", playlist.id)
            put("name", playlist.name)
            put("songs", JSONArray().apply {
                playlist.songs.forEach { song ->
                    put(JSONObject().apply {
                        put("id", song.id)
                    })
                }
            })
            put("coverArtUri", playlist.coverArtUri?.toString())
        }.toString()
    }

    private fun parsePlaylistJson(jsonString: String): Playlist {
        val json = JSONObject(jsonString)
        val songs = mutableListOf<Song>()
        val songsArray = json.getJSONArray("songs")
        for (i in 0 until songsArray.length()) {
            val songJson = songsArray.getJSONObject(i)
            songs.add(Song(id = songJson.getLong("id"), title = "", artist = "",
                path = "", duration = 0, albumId = 0))
        }

        return Playlist(
            id = json.getLong("id"),
            name = json.getString("name"),
            songs = songs,
            isUserCreated = true,
            coverArtUri = json.optString("coverArtUri")?.let { Uri.parse(it) }
        )
    }
}