package com.arsalankhan.vibemind

import android.app.ActivityOptions
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.arsalankhan.vibemind.databinding.ActivityMusicPlayerBinding
import com.bumptech.glide.Glide
import info.abdolahi.CircularMusicProgressBar
import info.abdolahi.OnCircularSeekBarChangeListener
import java.io.File
import java.io.Serializable

class MusicPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMusicPlayerBinding
    private lateinit var exoPlayer: ExoPlayer
    private var songList: ArrayList<Song> = arrayListOf()
    private var currentIndex = 0
    private var isShuffle = false
    private var isRepeat = false
    private var isLiked = false
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isUserSeeking) {
                val positionMs = exoPlayer.currentPosition
                val durationMs = exoPlayer.duration

                if (durationMs > 0) {
                    val progressPercent = (positionMs.toFloat() / durationMs.toFloat()) * 100f
                    binding.musicProgressBar.setValue(progressPercent)
                }

                binding.textCurrentTime.text = formatTime(positionMs)
                binding.textTotalTime.text = formatTime(durationMs)
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("UNCHECKED_CAST")
        songList = intent.getSerializableExtra("SONG_LIST") as? ArrayList<Song> ?: arrayListOf()
        currentIndex = intent.getIntExtra("SELECTED_INDEX", 0)

        setupExoPlayer()
        setupControls()

        if (songList.isNotEmpty()) {
            playSong(currentIndex)
        } else {
            Toast.makeText(this, "No songs to play", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 💡 NEW: This replaces the old onBackPressed() method
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle back press logic here
                overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)
                finish()
            }
        })

    }


    private fun setupExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    if (isRepeat) {
                        playSong(currentIndex)
                    } else {
                        skipToNext()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                binding.iconPlayPause.setImageResource(
                    if (isPlaying) R.drawable.ic_pause_circle else R.drawable.ic_play_circle
                )
                if (isPlaying) {
                    handler.post(updateRunnable)
                } else {
                    handler.removeCallbacks(updateRunnable)
                }
            }
        })
    }

    private fun playSong(index: Int) {
        if (index !in songList.indices) return

        val song = songList[index]
        val songUri = Uri.fromFile(File(song.path))
        Log.d("MusicPlayer", "Playing: ${song.title}, URI: $songUri")

        val mediaItem = MediaItem.fromUri(songUri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()

        Glide.with(this)
            .load(song.albumArtUri)
            .placeholder(R.drawable.album_art)
            .error(R.drawable.album_art)
            .into(binding.imageAlbumArt)

        binding.textSongTitle.text = song.title
        binding.textArtistName.text = song.artist
        binding.iconPlayPause.setImageResource(R.drawable.ic_pause_circle)

        isLiked = false
        binding.iconHeart.setImageResource(R.drawable.ic_heart_outline)
    }

    private fun setupControls() {
        binding.iconPlayPause.setOnClickListener {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
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

        // 💡 NEW: The close button now triggers the new back press dispatcher
        binding.iconClose.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.musicProgressBar.setOnCircularBarChangeListener(object : OnCircularSeekBarChangeListener {
            override fun onProgressChanged(
                circularBar: CircularMusicProgressBar?, progress: Int, fromUser: Boolean
            ) {
                if (fromUser) {
                    isUserSeeking = true
                    val durationMs = exoPlayer.duration
                    if (durationMs > 0) {
                        val seekPosition = (durationMs * (progress / 100f)).toLong()
                        binding.textCurrentTime.text = formatTime(seekPosition)
                    }
                }
            }

            override fun onClick(circularBar: CircularMusicProgressBar?) {}

            override fun onLongPress(circularBar: CircularMusicProgressBar?) {}


            fun onStopTrackingTouch(circularBar: CircularMusicProgressBar?) {
                val durationMs = exoPlayer.duration
                val progress = binding.musicProgressBar.javaClass.getDeclaredField("mProgressValue").apply { isAccessible = true }.get(binding.musicProgressBar) as Float
                if (durationMs > 0) {
                    val seekPosition = (durationMs * (progress / 100f)).toLong()
                    exoPlayer.seekTo(seekPosition)
                }
                isUserSeeking = false
            }
        })
    }

    private fun skipToNext() {
        currentIndex = if (isShuffle) {
            (songList.indices).random()
        } else {
            (currentIndex + 1) % songList.size
        }
        playSong(currentIndex)
    }

    private fun skipToPrevious() {
        currentIndex = if (currentIndex - 1 < 0) songList.size - 1 else currentIndex - 1
        playSong(currentIndex)
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        handler.removeCallbacks(updateRunnable)
    }
    override fun onBackPressed() {
        // Override to prevent default back press behavior
        // Use the custom back press dispatcher instead
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation)
    }

}