package com.arsalankhan.vibemind

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.Player
import com.arsalankhan.vibemind.databinding.ActivityMusicPlayerBinding
import com.bumptech.glide.Glide
import info.abdolahi.CircularMusicProgressBar
import info.abdolahi.OnCircularSeekBarChangeListener

class MusicPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMusicPlayerBinding
    private var songList: ArrayList<Song> = arrayListOf()
    private var currentIndex = 0
    private var isShuffle = false
    private var isRepeat = false
    private var isLiked = false

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            val positionMs = PlayerManager.getCurrentPosition()
            val durationMs = PlayerManager.getDuration()

            if (durationMs > 0) {
                val progressPercent = (positionMs.toFloat() / durationMs.toFloat()) * 100f
                binding.musicProgressBar.setValue(progressPercent)
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

        PlayerManager.init(this)

        @Suppress("UNCHECKED_CAST")
        songList = intent.getSerializableExtra("SONG_LIST") as? ArrayList<Song> ?: arrayListOf()
        currentIndex = intent.getIntExtra("SELECTED_INDEX", 0)

        setupControls()

        if (PlayerManager.currentSong == null || PlayerManager.currentIndex != currentIndex) {
            if (songList.isNotEmpty()) {
                PlayerManager.playSong(this, songList, currentIndex)
            } else {
                Toast.makeText(this, "No songs to play", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }

        updateUI(PlayerManager.currentSong!!)
        setupProgressBarListener()

        PlayerManager.addListener(object : Player.Listener {
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
        binding.iconPlayPause.setImageResource(R.drawable.ic_pause_circle)
        isLiked = false
        binding.iconHeart.setImageResource(R.drawable.ic_heart_outline)
    }

    private fun setupControls() {
        binding.iconPlayPause.setOnClickListener {
            if (PlayerManager.isPlaying()) {
                PlayerManager.pause()
            } else {
                PlayerManager.play()
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
    }

    private fun setupProgressBarListener() {
        binding.musicProgressBar.setOnCircularBarChangeListener(object : OnCircularSeekBarChangeListener {
            override fun onProgressChanged(circularBar: CircularMusicProgressBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val durationMs = PlayerManager.getDuration()
                    if (durationMs > 0) {
                        val seekPosition = (durationMs * (progress / 100f)).toLong()
                        PlayerManager.seekTo(seekPosition)
                    }
                }
            }

            override fun onClick(circularBar: CircularMusicProgressBar?) {}
            override fun onLongPress(circularBar: CircularMusicProgressBar?) {}
        })
    }

    private fun skipToNext() {
        currentIndex = if (isShuffle) {
            (songList.indices).random()
        } else {
            (currentIndex + 1) % songList.size
        }
        PlayerManager.playSong(this, songList, currentIndex)
        updateUI(songList[currentIndex])
    }

    private fun skipToPrevious() {
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }
}
