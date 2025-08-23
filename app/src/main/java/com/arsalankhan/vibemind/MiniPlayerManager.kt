package com.arsalankhan.vibemind

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.arsalankhan.vibemind.databinding.LayoutMiniPlayerBinding
import com.bumptech.glide.Glide
import java.lang.ref.WeakReference

object MiniPlayerManager {

    private var binding: LayoutMiniPlayerBinding? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    fun bindMiniPlayer(activity: Activity, miniBinding: LayoutMiniPlayerBinding) {
        binding = miniBinding
        currentActivityRef = WeakReference(activity)
        refresh(activity)
    }

    fun refresh(activity: Activity) {
        val song = PlayerManager.currentSong
        val binding = this.binding ?: return

        if (song == null) {
            binding.root.visibility = View.GONE
            return
        }

        binding.root.visibility = View.VISIBLE
        binding.tvSongTitle.text = song.title

        // Use the activity context directly in the refresh call
        Glide.with(activity)
            .load(song.getAlbumArtUri())
            .placeholder(R.drawable.ic_album_art)
            .into(binding.ivAlbumArt)

        // Update heart icon based on current liked status
        updateHeartIcon(song.isLiked)

        updatePlayPauseIcon()

        binding.ivPlayPause.setOnClickListener {
            if (PlayerManager.isPlaying()) {
                PlayerManager.pause()
            } else {
                PlayerManager.play()
            }
            updatePlayPauseIcon()
        }

        binding.ivHeart.setOnClickListener {
            val currentSong = PlayerManager.currentSong
            // Use the weak reference to get the context safely
            val context = currentActivityRef?.get() ?: return@setOnClickListener

            currentSong?.let { song ->
                val isCurrentlyLiked = song.isLiked
                if (isCurrentlyLiked) {
                    PlaylistManager.removeFromLikedSongs(context, song)
                    updateHeartIcon(false)
                    Toast.makeText(context, "Removed from Liked Songs", Toast.LENGTH_SHORT).show()
                } else {
                    PlaylistManager.addToLikedSongs(context, song)
                    updateHeartIcon(true)
                    Toast.makeText(context, "Added to Liked Songs", Toast.LENGTH_SHORT).show()
                }
                song.isLiked = !isCurrentlyLiked

                // Update the heart icon in MusicPlayerActivity if it's open
                updateMusicPlayerHeartIcon(song.isLiked)
            }
        }

        binding.root.setOnClickListener {
            if (PlayerManager.songList.isNotEmpty()) {
                val intent = Intent(activity, MusicPlayerActivity::class.java).apply {
                    putExtra("SONG_LIST", ArrayList(PlayerManager.songList))
                    putExtra("SELECTED_INDEX", PlayerManager.currentIndex)
                }
                activity.startActivity(intent)
            }
        }
    }

    private fun updatePlayPauseIcon() {
        binding?.ivPlayPause?.setImageResource(
            if (PlayerManager.isPlaying()) R.drawable.ic_pause_circle
            else R.drawable.ic_play_circle
        )
    }

    private fun updateHeartIcon(isLiked: Boolean) {
        binding?.ivHeart?.setImageResource(
            if (isLiked) R.drawable.ic_heart
            else R.drawable.ic_heart_outline
        )
    }

    private fun updateMusicPlayerHeartIcon(isLiked: Boolean) {
        // This method can be used to sync the heart icon with MusicPlayerActivity
        // You might want to implement a callback or event system for this
    }

    fun unbindMiniPlayer() {
        binding = null
        currentActivityRef?.clear()
        currentActivityRef = null
    }

    // Helper methods
    fun isMiniPlayerBound(): Boolean {
        return binding != null
    }

    fun getMiniPlayerBinding(): LayoutMiniPlayerBinding? {
        return binding
    }

    fun setMiniPlayerBinding(miniBinding: LayoutMiniPlayerBinding, activity: Activity) {
        binding = miniBinding
        currentActivityRef = WeakReference(activity)
    }

    fun clearMiniPlayerBinding() {
        binding = null
        currentActivityRef?.clear()
        currentActivityRef = null
    }

    fun isMiniPlayerVisible(): Boolean {
        return binding?.root?.visibility == View.VISIBLE
    }

    fun setMiniPlayerVisibility(visible: Boolean) {
        binding?.root?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun getMiniPlayerView(): View? {
        return binding?.root
    }

    fun getMiniPlayerSongTitle(): String? {
        return binding?.tvSongTitle?.text?.toString()
    }
}