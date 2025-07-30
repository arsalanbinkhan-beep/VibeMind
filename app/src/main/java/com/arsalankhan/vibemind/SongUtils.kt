package com.arsalankhan.vibemind

import android.content.Context
import android.provider.MediaStore
import java.io.File

class SongUtils {

    fun getAllAudioFiles(context: Context): ArrayList<Song> {
        val songList = ArrayList<Song>()
        val contentResolver = context.contentResolver

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(uri, projection, selection, null, sortOrder)

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: "Unknown Title"
                val artist = it.getString(artistColumn) ?: "Unknown Artist"
                val data = it.getString(dataColumn) ?: continue // Skip if no path
                val duration = it.getLong(durationColumn)
                val albumId = it.getLong(albumIdColumn)

                val file = File(data)
                if (!file.exists()) continue // Skip corrupted/missing files

                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    path = data,
                    duration = duration,
                    albumId = albumId,
                    category = inferCategory(title, artist)
                )

                songList.add(song)
            }
        }

        return songList
    }

    // 🔍 Basic keyword-based genre detection
    private fun inferCategory(title: String, artist: String): String {
        val combined = "$title $artist".lowercase()
        return when {
            "pop" in combined -> "Pop"
            "rock" in combined -> "Rock"
            "epic" in combined -> "Epic"
            "lofi" in combined || "lo-fi" in combined -> "Lofi"
            else -> "For You"
        }
    }
}
