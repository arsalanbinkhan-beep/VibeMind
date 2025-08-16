package com.arsalankhan.vibemind

import android.net.Uri
import java.io.Serializable

// Playlist.kt
data class Playlist(
    var id: Long,
    var name: String,
    var songs: MutableList<Song>,
    var artist: String = "Various Artists",
    var isUserCreated: Boolean = false,
    var coverArtUri: Uri? = null
) : Serializable {
    fun getCoverArt(): Uri {
        return coverArtUri ?: songs.firstOrNull()?.albumArtUri ?:
        Uri.parse("android.resource://com.arsalankhan.vibemind/drawable/album_art")
    }
}