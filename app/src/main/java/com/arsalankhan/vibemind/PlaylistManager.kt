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
    const val LIKED_PLAYLIST_ID = -1L
    const val MOST_PLAYED_PLAYLIST_ID = -2L
    const val FOR_YOU_PLAYLIST_ID = -3L

    private lateinit var prefs: SharedPreferences
    private var playlists = mutableListOf<Playlist>()
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadPlaylists(context)
        isInitialized = true
    }

    private fun loadPlaylists(context: Context) {
        playlists.clear()

        // Load user-created playlists first
        val savedPlaylists = prefs.getStringSet("user_playlists", setOf()) ?: setOf()
        savedPlaylists.forEach { json ->
            try {
                val playlist = parsePlaylistJson(json)
                if (playlists.none { it.id == playlist.id }) {
                    playlists.add(playlist)
                }
            } catch (e: Exception) {
                Log.e("PlaylistManager", "Error parsing playlist: ${e.message}")
            }
        }

        // Then add default playlists (ensure they don't already exist)
        if (playlists.none { it.id == LIKED_PLAYLIST_ID }) {
            playlists.add(Playlist(LIKED_PLAYLIST_ID, "Liked Songs", mutableListOf(), "Your favorite tracks"))
        }
        if (playlists.none { it.id == MOST_PLAYED_PLAYLIST_ID }) {
            playlists.add(Playlist(MOST_PLAYED_PLAYLIST_ID, "Most Played", mutableListOf(), "Your top tracks"))
        }
        if (playlists.none { it.id == FOR_YOU_PLAYLIST_ID }) {
            playlists.add(Playlist(FOR_YOU_PLAYLIST_ID, "For You", mutableListOf(), "Based on your listening"))
        }

        // Load liked songs into the Liked Songs playlist
        loadLikedSongs(context)
    }

    private fun loadLikedSongs(context: Context) {
        val likedSongsPrefs = context.getSharedPreferences("liked_songs", Context.MODE_PRIVATE)
        val likedSongIds = likedSongsPrefs.getStringSet("liked_song_ids", setOf()) ?: setOf()

        val likedPlaylist = getLikedSongsPlaylist()
        likedPlaylist.songs.clear()

        val allSongs = SongUtils().getAllAudioFiles(context)

        likedSongIds.forEach { songId ->
            val song = allSongs.find { it.id.toString() == songId }
            song?.let {
                it.isLiked = true
                likedPlaylist.songs.add(it)
            }
        }
    }

    fun saveUserPlaylists() {
        val userPlaylists = playlists.filter { it.isUserCreated }
        val jsonSet = userPlaylists.map { playlistToJson(it) }.toSet()
        prefs.edit().putStringSet("user_playlists", jsonSet).apply()
    }

    fun saveLikedSongs(context: Context) {
        val likedPlaylist = getLikedSongsPlaylist()
        val likedSongIds = likedPlaylist.songs.map { it.id.toString() }.toSet()
        val likedSongsPrefs = context.getSharedPreferences("liked_songs", Context.MODE_PRIVATE)
        likedSongsPrefs.edit().putStringSet("liked_song_ids", likedSongIds).apply()
    }

    fun addToLikedSongs(context: Context, song: Song) {
        val likedPlaylist = getLikedSongsPlaylist()
        if (!likedPlaylist.songs.any { it.id == song.id }) {
            song.isLiked = true
            likedPlaylist.songs.add(song)
            saveLikedSongs(context)
        }
    }

    fun removeFromLikedSongs(context: Context, song: Song) {
        val likedPlaylist = getLikedSongsPlaylist()
        likedPlaylist.songs.removeAll { it.id == song.id }
        song.isLiked = false
        saveLikedSongs(context)
    }


    fun createNewPlaylist(context: Context, name: String, initialSongs: List<Song> = emptyList()): Playlist {
        val newId = System.currentTimeMillis()
        val newPlaylist = Playlist(newId, name, initialSongs.toMutableList(), "You", true)

        if (playlists.any { it.name == name && it.isUserCreated }) {
            throw IllegalArgumentException("Playlist with name '$name' already exists")
        }

        playlists.add(newPlaylist)
        saveUserPlaylists()
        return newPlaylist
    }

    fun addSongToPlaylist(context: Context, playlistId: Long, song: Song) {
        val playlist = playlists.firstOrNull { it.id == playlistId }
        playlist?.songs?.add(song)
        if (playlist?.isUserCreated == true) {
            saveUserPlaylists()
        }
    }

    fun removeSongFromPlaylist(context: Context, playlistId: Long, songId: Long) {
        val playlist = playlists.firstOrNull { it.id == playlistId }
        playlist?.songs?.removeAll { it.id == songId }
        if (playlist?.isUserCreated == true) {
            saveUserPlaylists()
        }
    }

    // --------- Methods from 2nd code snippet ---------

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
        return playlists.first { it.id == LIKED_PLAYLIST_ID }
    }

    fun getUserPlaylists(): List<Playlist> {
        return playlists.filter { it.isUserCreated }
    }

    private fun playlistToJson(playlist: Playlist): String {
        return JSONObject().apply {
            put("id", playlist.id)
            put("name", playlist.name)
            put("artist", playlist.artist)
            put("isUserCreated", playlist.isUserCreated)
            put("coverArtUri", playlist.coverArtUri?.toString())
            put("songs", JSONArray().apply {
                playlist.songs.forEach { song ->
                    put(JSONObject().apply {
                        put("id", song.id)
                        put("title", song.title)
                        put("artist", song.artist)
                        put("path", song.path)
                        put("duration", song.duration)
                        put("albumId", song.albumId)
                        put("isLiked", song.isLiked)
                    })
                }
            })
        }.toString()
    }

    private fun parsePlaylistJson(jsonString: String): Playlist {
        val json = JSONObject(jsonString)
        val songs = mutableListOf<Song>()
        val songsArray = json.getJSONArray("songs")

        for (i in 0 until songsArray.length()) {
            val songJson = songsArray.getJSONObject(i)
            songs.add(Song(
                id = songJson.getLong("id"),
                title = songJson.getString("title"),
                artist = songJson.getString("artist"),
                path = songJson.getString("path"),
                duration = songJson.getLong("duration"),
                albumId = songJson.getLong("albumId"),
                isLiked = songJson.optBoolean("isLiked", false)
            ))
        }

        return Playlist(
            id = json.getLong("id"),
            name = json.getString("name"),
            songs = songs,
            artist = json.optString("artist", "You"),
            isUserCreated = json.optBoolean("isUserCreated", true),
            coverArtUri = json.optString("coverArtUri")?.let { Uri.parse(it) }
        )
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        val playlist = playlists.firstOrNull { it.id == playlistId }
        playlist?.songs?.add(song)
        saveUserPlaylists()
    }

    fun getPlaylistById(playlistId: Long): Playlist? {
        return playlists.firstOrNull { it.id == playlistId && it.isUserCreated }
    }

    fun deletePlaylist(playlistId: Long) {
        playlists.removeAll { it.id == playlistId && it.isUserCreated }
        saveUserPlaylists()
    }

    fun canDeletePlaylist(playlistId: Long): Boolean {
        return playlists.any { it.id == playlistId && it.isUserCreated }
    }

    fun getArtistPlaylists(allSongs: List<Song>): List<Playlist> {
        val artistPlaylists = mutableListOf<Playlist>()
        val songsByArtist = allSongs.groupBy { it.artist }

        songsByArtist.forEach { (artist, songs) ->
            if (songs.size >= 2) {
                val playlist = Playlist(
                    id = System.currentTimeMillis() + artist.hashCode().toLong(),
                    name = artist,
                    songs = songs.toMutableList(),
                    artist = artist,
                    isUserCreated = false,
                    isArtistPlaylist = true
                )
                artistPlaylists.add(playlist)
            }
        }
        return artistPlaylists.sortedBy { it.name }
    }
    // Add this method to refresh the liked status when loading songs
    fun refreshLikedStatus(context: Context, songs: List<Song>) {
        val likedSongsPrefs = context.getSharedPreferences("liked_songs", Context.MODE_PRIVATE)
        val likedSongIds = likedSongsPrefs.getStringSet("liked_song_ids", setOf()) ?: setOf()

        songs.forEach { song ->
            song.isLiked = likedSongIds.contains(song.id.toString())
        }
    }
    fun getPlaylistById(context: Context, playlistId: Long, allSongs: List<Song> = emptyList()): Playlist? {
        return when (playlistId) {
            LIKED_PLAYLIST_ID -> getLikedSongsPlaylist()
            MOST_PLAYED_PLAYLIST_ID -> getMostPlayedPlaylist(context, allSongs)
            FOR_YOU_PLAYLIST_ID -> getForYouPlaylist(context, allSongs)
            else -> playlists.firstOrNull { it.id == playlistId }
        }
    }
}
