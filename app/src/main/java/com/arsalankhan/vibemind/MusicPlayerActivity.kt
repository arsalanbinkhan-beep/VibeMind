package com.arsalankhan.vibemind

import android.content.ContentUris
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
import kotlin.math.sqrt
private lateinit var sensorManager: SensorManager
private var lastShakeTime = 0L
private lateinit var sensorListener: SensorEventListener

@Suppress("DEPRECATION")
class MusicPlayerActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMusicPlayerBinding
    private var songList: ArrayList<Song> = arrayListOf()
    private var currentIndex = 0
    private var isShuffle = false
    private var isRepeat = false
    private var isLiked = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences // 🔥 CHANGED

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
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val now = System.currentTimeMillis()

                if (acceleration > 15 && now - lastShakeTime > 1000) {
                    lastShakeTime = now
                    skipToNext() // 👈 Your existing method
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)


        PlayerManager.init(this)
        prefs = getSharedPreferences("music_prefs", MODE_PRIVATE) // 🔥 CHANGED

        @Suppress("UNCHECKED_CAST")
        songList = intent.getSerializableExtra("SONG_LIST") as? ArrayList<Song> ?: arrayListOf()
        currentIndex = intent.getIntExtra("SELECTED_INDEX", -1) // 🔥 CHANGED (default -1)

        setupControls()
        setupCircularTouchControl()

        if (songList.isEmpty()) {
            Toast.makeText(this, "No songs to play", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (PlayerManager.currentSong == null) {
            if (currentIndex == -1) {
                // 🔥 No song selected → restore last state
                val lastIndex = prefs.getInt("last_index", 0)
                val lastPosition = prefs.getLong("last_position", 0L)

                PlayerManager.prepareSong(this, songList, lastIndex)
                PlayerManager.seekTo(lastPosition)
            } else {
                // 🔥 Start fresh selection
                PlayerManager.prepareSong(this, songList, currentIndex)
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
                    if (isRepeat) {
                        PlayerManager.playSong(this@MusicPlayerActivity, songList, currentIndex)
                    } else {
                        skipToNext()
                    }
                }
            }
        })
    }


    private fun updateUI(song: Song) {
        val albumArtUri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            song.albumId
        )

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
    }

    private fun setupControls() {
        binding.iconPlayPause.setOnClickListener {
            if (PlayerManager.isPlaying()) PlayerManager.pause()
            else PlayerManager.play()
        }

        binding.iconNext.setOnClickListener { skipToNext() }
        binding.iconPrevious.setOnClickListener { skipToPrevious() }

        binding.iconShuffle.setOnClickListener {
            isShuffle = !isShuffle
            binding.iconShuffle.setImageResource(
                if (isShuffle) R.drawable.ic_shuffle_active else R.drawable.ic_shuffle
            )
        }

        binding.iconRepeat.setOnClickListener {
            isRepeat = !isRepeat
            binding.iconRepeat.setImageResource(
                if (isRepeat) R.drawable.ic_repeat_active else R.drawable.ic_repeat
            )
        }

        binding.iconHeart.setOnClickListener {
            isLiked = !isLiked
            binding.iconHeart.setImageResource(
                if (isLiked) R.drawable.ic_heart else R.drawable.ic_heart_outline
            )
        }
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
        currentIndex = if (isShuffle) (songList.indices).random()
        else (currentIndex + 1) % songList.size

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
            .apply()

        // ✅ Important: unregister the shake listener
        sensorManager.unregisterListener(sensorListener)
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }
    companion object {
        fun skipToNext(context: android.content.Context) {
            if (PlayerManager.songList.isNotEmpty()) {
                val nextIndex = (PlayerManager.currentIndex + 1) % PlayerManager.songList.size
                PlayerManager.playSong(context, PlayerManager.songList, nextIndex)
            }
        }
    }

}
