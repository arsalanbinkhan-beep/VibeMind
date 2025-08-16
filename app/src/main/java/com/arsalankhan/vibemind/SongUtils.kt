package com.arsalankhan.vibemind

import android.content.Context
import android.provider.MediaStore
import java.io.File

class SongUtils {

    fun getAllAudioFiles(context: Context): ArrayList<Song> {
        val songList = ArrayList<Song>()
        val contentResolver = context.contentResolver
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val showHidden = prefs.getBoolean("show_hidden", false)

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // Add hidden files filter if needed
        var selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        if (!showHidden) {
            selection += " AND ${MediaStore.Audio.Media.DATA} NOT LIKE '%.%'"
        }

        // Get sort order from preferences
        val sortOrder = when (prefs.getInt("sort_order", 0)) {
            0 -> MediaStore.Audio.Media.TITLE + " ASC"
            1 -> MediaStore.Audio.Media.ARTIST + " ASC"
            2 -> MediaStore.Audio.Media.DATE_ADDED + " DESC"
            else -> MediaStore.Audio.Media.TITLE + " ASC"
        }

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
                val data = it.getString(dataColumn) ?: continue
                val duration = it.getLong(durationColumn)
                val albumId = it.getLong(albumIdColumn)

                val file = File(data)
                if (!file.exists()) continue

                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    path = data,
                    duration = duration,
                    albumId = albumId
                    // The category is no longer set here.
                )

                songList.add(song)
            }
        }
        return songList
    }
}