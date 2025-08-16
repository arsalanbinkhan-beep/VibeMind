package com.arsalankhan.vibemind

import android.net.Uri
import java.io.Serializable

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long,
    val albumId: Long,
    var isLiked: Boolean = false // Add this field
) : Serializable {
    val albumArtUri: Uri
        get() = Uri.parse("content://media/external/audio/albumart").buildUpon()
            .appendPath(albumId.toString()).build()
}
