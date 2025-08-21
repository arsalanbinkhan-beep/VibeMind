package com.arsalankhan.vibemind

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// MusicNotificationService.kt
class MusicNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 101
        const val ACTION_PLAY_PAUSE = "play_pause"
        const val ACTION_NEXT = "next"
        const val ACTION_PREVIOUS = "previous"
        const val ACTION_CLOSE = "close"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY_PAUSE -> {
                    if (PlayerManager.isPlaying()) {
                        PlayerManager.pause()
                    } else {
                        PlayerManager.play()
                    }
                }
                ACTION_NEXT -> {
                    MusicPlayerActivity.skipToNext(this)
                }
                ACTION_PREVIOUS -> {
                    MusicPlayerActivity.skipToPrevious(this)
                }
                ACTION_CLOSE -> {
                    stopForeground(true)
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }

        // Always update notification
        updateNotification()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW // Use LOW instead of HIGH for persistent notification
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification() {
        val song = PlayerManager.currentSong ?: run {
            stopForeground(true)
            return
        }

        val isPlaying = PlayerManager.isPlaying()
        val showMediaControls = prefs.getBoolean("show_media_controls", true)

        if (!showMediaControls) {
            stopForeground(true)
            return
        }

        // Create pending intents
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MusicPlayerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicNotificationService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicNotificationService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val previousIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MusicNotificationService::class.java).setAction(ACTION_PREVIOUS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val closeIntent = PendingIntent.getService(
            this, 4,
            Intent(this, MusicNotificationService::class.java).setAction(ACTION_CLOSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build basic notification first (without album art)
        val basicNotification = buildBasicNotification(song, isPlaying, contentIntent, playPauseIntent, nextIntent, previousIntent, closeIntent)
        startForeground(NOTIFICATION_ID, basicNotification)

        // Then load album art asynchronously and update
        loadAlbumArtAndUpdateNotification(song, isPlaying, contentIntent, playPauseIntent, nextIntent, previousIntent, closeIntent)
    }

    private fun buildBasicNotification(
        song: Song,
        isPlaying: Boolean,
        contentIntent: PendingIntent,
        playPauseIntent: PendingIntent,
        nextIntent: PendingIntent,
        previousIntent: PendingIntent,
        closeIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Use LOW for persistent notification
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(NotificationCompat.Action(
                android.R.drawable.ic_media_previous, "Previous", previousIntent
            ))
            .addAction(NotificationCompat.Action(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            ))
            .addAction(NotificationCompat.Action(
                android.R.drawable.ic_media_next, "Next", nextIntent
            ))
            .addAction(NotificationCompat.Action(
                android.R.drawable.ic_menu_close_clear_cancel, "Close", closeIntent
            ))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setShowCancelButton(true)
                .setCancelButtonIntent(closeIntent))
            .build()
    }

    private fun loadAlbumArtAndUpdateNotification(
        song: Song,
        isPlaying: Boolean,
        contentIntent: PendingIntent,
        playPauseIntent: PendingIntent,
        nextIntent: PendingIntent,
        previousIntent: PendingIntent,
        closeIntent: PendingIntent
    ) {
        Glide.with(this)
            .asBitmap()
            .load(song.getAlbumArtUri())
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val notification = NotificationCompat.Builder(this@MusicNotificationService, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_music_note)
                        .setContentTitle(song.title)
                        .setContentText(song.artist)
                        .setContentIntent(contentIntent)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setOnlyAlertOnce(true)
                        .setOngoing(isPlaying)
                        .setShowWhen(false)
                        .setAutoCancel(false)
                        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                        .setLargeIcon(resource)
                        .addAction(NotificationCompat.Action(
                            android.R.drawable.ic_media_previous, "Previous", previousIntent
                        ))
                        .addAction(NotificationCompat.Action(
                            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                            if (isPlaying) "Pause" else "Play",
                            playPauseIntent
                        ))
                        .addAction(NotificationCompat.Action(
                            android.R.drawable.ic_media_next, "Next", nextIntent
                        ))
                        .addAction(NotificationCompat.Action(
                            android.R.drawable.ic_menu_close_clear_cancel, "Close", closeIntent
                        ))
                        .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1, 2)
                            .setShowCancelButton(true)
                            .setCancelButtonIntent(closeIntent))
                        .build()

                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }

                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                    // Keep the basic notification without album art
                }
            })
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }
}