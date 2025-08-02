package com.arsalankhan.vibemind

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
    private val updateRunnable = object : Runnable {
        override fun run() {
            val positionMs = exoPlayer.currentPosition
            val durationMs = exoPlayer.duration

            if (durationMs > 0) {
                // Update the circular progress bar
                val progress = (positionMs.toFloat() / durationMs.toFloat()) * 100f
                binding.musicProgressBar.progress = progress
            }

            // Update the TextViews for time
            binding.textCurrentTime.text = formatTime(positionMs)
            binding.textTotalTime.text = formatTime(durationMs)

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get song list and selected index from intent
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

        // Update UI text and controls
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

        // Listener for the circular progress bar to handle seeking
        binding.musicProgressBar.setOnCircularBarChangeListener(object : OnCircularSeekBarChangeListener {
            override fun onProgressChanged(circularBar: CircularMusicProgressBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val durationMs = exoPlayer.duration
                    if (durationMs > 0) {
                        val seekPosition = (durationMs * (progress / 100f)).toLong()
                        exoPlayer.seekTo(seekPosition)
                    }
                }
            }

            override fun onClick(circularBar: CircularMusicProgressBar?) {
                // Not implementing a click action for the progress bar
            }

            override fun onLongPress(circularBar: CircularMusicProgressBar?) {
                // Not implementing a long press action for the progress bar
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
    }
}