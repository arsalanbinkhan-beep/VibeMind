package com.arsalankhan.vibemind

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

object PlayerManager {
    private lateinit var exoPlayer: ExoPlayer
    var currentSong: Song? = null
    var songList: List<Song> = listOf()
    var currentIndex: Int = 0

    fun init(context: Context) {
        if (!::exoPlayer.isInitialized) {
            exoPlayer = ExoPlayer.Builder(context.applicationContext).build()
        }
    }

    fun playSong(context: Context, list: List<Song>, index: Int) {
        init(context)
        if (index !in list.indices) return

        currentSong = list[index]
        songList = list
        currentIndex = index

        val mediaItem = MediaItem.fromUri(currentSong!!.path)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun pause() {
        if (::exoPlayer.isInitialized) exoPlayer.pause()
    }

    fun play() {
        if (::exoPlayer.isInitialized) exoPlayer.play()
    }

    fun isPlaying(): Boolean = ::exoPlayer.isInitialized && exoPlayer.isPlaying

    fun getCurrentPosition(): Long = if (::exoPlayer.isInitialized) exoPlayer.currentPosition else 0L

    fun getDuration(): Long = if (::exoPlayer.isInitialized) exoPlayer.duration else 0L

    fun seekTo(positionMs: Long) {
        if (::exoPlayer.isInitialized) exoPlayer.seekTo(positionMs)
    }

    fun addListener(listener: androidx.media3.common.Player.Listener) {
        if (::exoPlayer.isInitialized) exoPlayer.addListener(listener)
    }

    fun removeListener(listener: androidx.media3.common.Player.Listener) {
        if (::exoPlayer.isInitialized) exoPlayer.removeListener(listener)
    }

    fun release() {
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
    }
    fun prepareSong(context: Context, songs: List<Song>, index: Int) {
        currentIndex = index
        currentSong = songs[index]

        val songUri = Uri.parse(currentSong!!.path) // or currentSong!!.filePath
        exoPlayer?.setMediaItem(MediaItem.fromUri(songUri))
        exoPlayer?.prepare()
    }


}
