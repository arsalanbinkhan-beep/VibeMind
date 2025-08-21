package com.arsalankhan.vibemind

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.Player
import com.arsalankhan.vibemind.databinding.ActivityMusicPlayerBinding
import com.bumptech.glide.Glide
import kotlin.math.atan2
import kotlin.math.roundToInt
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.arsalankhan.vibemind.PlayerManager.currentSong
import kotlin.math.sqrt
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

@Suppress("DEPRECATION")
class MusicPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMusicPlayerBinding
    private var songList: ArrayList<Song> = arrayListOf()
    private var currentIndex = 0
    private var isShuffle = false
    private var isLiked = false

    // Repeat mode variables
    private var repeatMode = 0 // 0 = off, 1 = repeat all, 2 = repeat one
    private val REPEAT_OFF = 0
    private val REPEAT_ALL = 1
    private val REPEAT_ONE = 2

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences

    // Sensor-related properties
    private lateinit var sensorManager: SensorManager
    private lateinit var sensorListener: SensorEventListener
    private var lastShakeTime = 0L

    private val updateRunnable = object : Runnable {
        override fun run() {
            val positionMs = PlayerManager.getCurrentPosition()
            val durationMs = PlayerManager.getDuration()

            if (durationMs > 0) {
                val progressPercent = (positionMs.toFloat() / durationMs) * 100f
                binding.circularProgress.progress = progressPercent.toInt()
            }

            binding.textCurrentTime.text = formatTime(positionMs)
            binding.textTotalTime.text = formatTime(durationMs)

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize sensor manager and listener
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
                val shakeEnabled = prefs.getBoolean("shake_to_change", false)

                if (!shakeEnabled) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val now = System.currentTimeMillis()
                val sensitivity = prefs.getInt("shake_sensitivity", 50)
                val threshold = 15 - (sensitivity / 10f)

                if (acceleration > threshold && now - lastShakeTime > 1000) {
                    lastShakeTime = now
                    skipToNext()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        PlayerManager.init(this)
        prefs = getSharedPreferences("music_prefs", MODE_PRIVATE)

        @Suppress("UNCHECKED_CAST")
        songList = intent.getSerializableExtra("SONG_LIST") as? ArrayList<Song> ?: arrayListOf()
        currentIndex = intent.getIntExtra("SELECTED_INDEX", -1)

        // Restore repeat mode from preferences
        repeatMode = prefs.getInt("repeat_mode", REPEAT_OFF)

        setupControls()
        setupCircularTouchControl()

        if (songList.isEmpty()) {
            Toast.makeText(this, "No songs to play", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (PlayerManager.currentSong == null) {
            if (currentIndex == -1) {
                val lastIndex = prefs.getInt("last_index", 0)
                val lastPosition = prefs.getLong("last_position", 0L)
                PlayerManager.prepareSong(this, songList, lastIndex)
                PlayerManager.seekTo(lastPosition)
            } else {
                PlayerManager.prepareSong(this, songList, currentIndex)
            }
        }

        // Apply the saved repeat mode to the player
        when (repeatMode) {
            REPEAT_OFF -> {
                PlayerManager.exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            }
            REPEAT_ALL -> {
                PlayerManager.exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
            }
            REPEAT_ONE -> {
                PlayerManager.exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            }
        }

        PlayerManager.currentSong?.let { updateUI(it) }

        PlayerManager.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                binding.iconPlayPause.setImageResource(
                    if (isPlaying) R.drawable.ic_pause_circle else R.drawable.ic_play_circle
                )
                if (isPlaying) handler.post(updateRunnable)
                else handler.removeCallbacks(updateRunnable)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    when (repeatMode) {
                        REPEAT_ONE -> {
                            // For repeat one, just seek to beginning and play again
                            PlayerManager.seekTo(0)
                            PlayerManager.play()
                        }
                        REPEAT_ALL -> {
                            // For repeat all, go to next song (which will loop to beginning if at end)
                            skipToNext()
                        }
                        else -> {
                            // For no repeat, just go to next song normally
                            skipToNext()
                        }
                    }
                }
            }
        })
        NotificationManager.startNotificationService(this)
    }

    override fun onResume() {
        super.onResume()
        setupShakeDetection()
    }

    private fun updateUI(song: Song) {
        val albumArtUri = song.getAlbumArtUri()

        Glide.with(this)
            .load(albumArtUri)
            .placeholder(R.drawable.album_art)
            .error(R.drawable.album_art)
            .into(binding.imageAlbumArt)

        binding.textSongTitle.text = song.title
        binding.textArtistName.text = song.artist
        binding.iconPlayPause.setImageResource(
            if (PlayerManager.isPlaying()) R.drawable.ic_pause_circle else R.drawable.ic_play_circle
        )

        isLiked = false
        binding.iconHeart.setImageResource(R.drawable.ic_heart_outline)

        val durationMs = PlayerManager.getDuration()
        binding.textTotalTime.text = formatTime(durationMs)

        val positionMs = PlayerManager.getCurrentPosition()
        binding.textCurrentTime.text = formatTime(positionMs)
        if (durationMs > 0) {
            val progressPercent = (positionMs.toFloat() / durationMs) * 100f
            binding.circularProgress.progress = progressPercent.toInt()
        }

        // Update icons
        updateRepeatIcon()
        updateShuffleIcon()
    }

    private fun setupControls() {
        binding.iconPlayPause.setOnClickListener {
            if (PlayerManager.isPlaying()) PlayerManager.pause()
            else PlayerManager.play()
        }
        binding.iconAddToPlaylist.setOnClickListener {
            currentSong?.let { song ->
                showAddToPlaylistDialog(song)
            }
        }

        binding.iconNext.setOnClickListener { skipToNext() }
        binding.iconPrevious.setOnClickListener { skipToPrevious() }

        binding.iconShuffle.setOnClickListener {
            isShuffle = !isShuffle
            updateShuffleIcon()
        }

        binding.iconRepeat.setOnClickListener {
            cycleRepeatMode()
            updateRepeatIcon()
        }

        binding.iconRepeat.setOnLongClickListener {
            val message = when (repeatMode) {
                REPEAT_OFF -> "Repeat off"
                REPEAT_ALL -> "Repeat all"
                REPEAT_ONE -> "Repeat one"
                else -> "Repeat"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            true
        }

        binding.iconHeart.setOnClickListener {
            isLiked = !isLiked
            currentSong?.let { song ->
                if (isLiked) {
                    PlaylistManager.addToLikedSongs(song)
                    binding.iconHeart.setImageResource(R.drawable.ic_heart)
                    Toast.makeText(this, "Added to Liked Songs", Toast.LENGTH_SHORT).show()
                } else {
                    PlaylistManager.removeFromLikedSongs(song)
                    binding.iconHeart.setImageResource(R.drawable.ic_heart_outline)
                    Toast.makeText(this, "Removed from Liked Songs", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            REPEAT_OFF -> REPEAT_ALL
            REPEAT_ALL -> REPEAT_ONE
            else -> REPEAT_OFF
        }

        // Apply the repeat mode to the player
        when (repeatMode) {
            REPEAT_OFF -> {
                PlayerManager.exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            }
            REPEAT_ALL -> {
                PlayerManager.exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
            }
            REPEAT_ONE -> {
                PlayerManager.exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            }
        }
    }

    private fun updateRepeatIcon() {
        val iconResource = when (repeatMode) {
            REPEAT_OFF -> R.drawable.ic_repeat
            REPEAT_ALL -> R.drawable.ic_repeat_active
            REPEAT_ONE -> R.drawable.ic_repeat_one
            else -> R.drawable.ic_repeat
        }
        binding.iconRepeat.setImageResource(iconResource)
    }

    private fun updateShuffleIcon() {
        binding.iconShuffle.setImageResource(
            if (isShuffle) R.drawable.ic_shuffle_active else R.drawable.ic_shuffle
        )
    }

    private fun setupCircularTouchControl() {
        binding.circularProgress.setOnTouchListener { v, event ->
            val centerX = v.width / 2f
            val centerY = v.height / 2f
            val dx = event.x - centerX
            val dy = event.y - centerY

            var degrees = Math.toDegrees(atan2(dy, dx).toDouble())
            degrees = (degrees + 360 + 90) % 360
            val newProgress = (degrees / 360 * 100).roundToInt().coerceIn(0, 100)

            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                binding.circularProgress.progress = newProgress
                val durationMs = PlayerManager.getDuration()
                if (durationMs > 0) {
                    val seekPosition = (durationMs * (newProgress / 100f)).toLong()
                    PlayerManager.seekTo(seekPosition)
                    binding.textCurrentTime.text = formatTime(seekPosition)
                }
            }
            true
        }
    }

    fun skipToNext() {
        when (repeatMode) {
            REPEAT_ALL -> {
                // For repeat all, loop back to beginning if at end
                currentIndex = if (currentIndex + 1 >= songList.size) 0 else currentIndex + 1
            }
            else -> {
                // Normal behavior for other modes
                currentIndex = if (isShuffle) (songList.indices).random()
                else (currentIndex + 1) % songList.size
            }
        }

        PlayerManager.playSong(this, songList, currentIndex)
        updateUI(songList[currentIndex])
    }

    fun skipToPrevious() {
        currentIndex = if (currentIndex - 1 < 0) songList.size - 1 else currentIndex - 1
        PlayerManager.playSong(this, songList, currentIndex)
        updateUI(songList[currentIndex])
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        prefs.edit()
            .putInt("last_index", PlayerManager.currentIndex)
            .putLong("last_position", PlayerManager.getCurrentPosition())
            .putInt("repeat_mode", repeatMode) // Save repeat mode
            .apply()

        // Unregister the shake listener
        sensorManager.unregisterListener(sensorListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        if (!PlayerManager.isPlaying()) {
            NotificationManager.stopNotificationService(this)
        }
    }

    private fun setupShakeDetection() {
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val shakeEnabled = prefs.getBoolean("shake_to_change", false)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Log.w("MusicPlayerActivity", "Accelerometer not available on this device.")
            return
        }

        if (shakeEnabled) {
            sensorManager.registerListener(
                sensorListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    private fun showAddToPlaylistDialog(song: Song) {
        val playlists = PlaylistManager.getUserPlaylists()

        if (playlists.isEmpty()) {
            Toast.makeText(this, "Create a playlist first", Toast.LENGTH_SHORT).show()
            return
        }

        // Create custom dialog with playlist items showing album art
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_playlist_selector, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewPlaylists)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = PlaylistAdapter(playlists) { selectedPlaylist ->
            if (!selectedPlaylist.songs.any { it.id == song.id }) {
                selectedPlaylist.songs.add(song)
                PlaylistManager.saveUserPlaylists()
                Toast.makeText(this, "Added to ${selectedPlaylist.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Song already in playlist", Toast.LENGTH_SHORT).show()
            }
            // Dismiss dialog after selection
            // You'll need to keep a reference to the dialog to dismiss it
        }

        recyclerView.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Add to playlist")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .show()
    }
    // Add this companion object method to MusicPlayerActivity
    companion object {
        fun skipToNext(context: Context) {
            try {
                if (PlayerManager.songList.isNotEmpty() && PlayerManager.songList.size > 1) {
                    val nextIndex = (PlayerManager.currentIndex + 1) % PlayerManager.songList.size
                    PlayerManager.playSong(context, PlayerManager.songList, nextIndex)
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerActivity", "Error skipping to next song: ${e.message}")
            }
        }

        fun skipToPrevious(context: Context) {
            try {
                if (PlayerManager.songList.isNotEmpty() && PlayerManager.songList.size > 1) {
                    val previousIndex = if (PlayerManager.currentIndex - 1 < 0) {
                        PlayerManager.songList.size - 1
                    } else {
                        PlayerManager.currentIndex - 1
                    }
                    PlayerManager.playSong(context, PlayerManager.songList, previousIndex)
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerActivity", "Error skipping to previous song: ${e.message}")
            }
        }
    }

}