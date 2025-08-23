package com.arsalankhan.vibemind

import android.content.Context
import android.os.Looper
import androidx.core.os.postDelayed
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

object PlayerManager {

    // 💡 FIX: Use only one ExoPlayer instance. It's nullable for safety.
    private var player: ExoPlayer? = null
    var currentSong: Song? = null
    var songList: List<Song> = listOf()
    var currentIndex: Int = 0
    var onSongChanged: ((Song) -> Unit)? = null

    // Store application context
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        if (player == null) {
            player = ExoPlayer.Builder(appContext).build()
        }
    }

    fun playSong(context: Context, list: List<Song>, index: Int) {
        init(context) // Ensure player is initialized
        if (index !in list.indices) return

        currentSong = list[index]
        songList = list
        currentIndex = index

        onSongChanged?.invoke(currentSong!!)

        val mediaItem = MediaItem.fromUri(currentSong!!.path)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()

        android.os.Handler(Looper.getMainLooper()).postDelayed({
            NotificationManager.startNotificationService(context)
        }, 100)
    }

    fun pause() {
        player?.pause()
        if (::appContext.isInitialized && currentSong != null) {
            NotificationManager.updateNotification(appContext)
        }
    }

    fun play() {
        player?.play()
        if (::appContext.isInitialized && currentSong != null) {
            NotificationManager.updateNotification(appContext)
        }
    }

    fun isPlaying(): Boolean = player?.isPlaying ?: false

    fun getCurrentPosition(): Long = player?.currentPosition ?: 0L

    fun getDuration(): Long = player?.duration ?: 0L

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    // ✅ FIXED: These methods now use the correct 'player' instance
    fun addListener(listener: Player.Listener) {
        player?.addListener(listener)
    }

    // ✅ FIXED: These methods now use the correct 'player' instance
    fun removeListener(listener: Player.Listener) {
        player?.removeListener(listener)
    }

    // ✅ FIXED: This method now uses the correct 'player' instance
    fun release() {
        player?.release()
        player = null
    }

    // 💡 NOTE: This method seems to be a duplicate of playSong.
    // Consider removing it and calling playSong directly.
    fun prepareSong(context: Context, songs: List<Song>, index: Int) {
        currentIndex = index
        currentSong = songs[index]
        val songUri = currentSong!!.path
        player?.setMediaItem(MediaItem.fromUri(songUri))
        player?.prepare()
    }

    // ✅ FIXED: This method now returns the correct audio session ID from the player
    fun getAudioSessionId(): Int {
        return player?.audioSessionId ?: -1
    }

    // 💡 NEW: Method to get the player instance, used by SettingsActivity
    fun getPlayer(): ExoPlayer? {
        return player
    }
}