package com.arsalankhan.vibemind

import android.app.Activity
import android.content.Intent
import android.view.View
import com.arsalankhan.vibemind.databinding.LayoutMiniPlayerBinding
import com.bumptech.glide.Glide

object MiniPlayerManager {

    private var binding: LayoutMiniPlayerBinding? = null

    fun bindMiniPlayer(activity: Activity, miniBinding: LayoutMiniPlayerBinding) {
        binding = miniBinding

        val song = PlayerManager.currentSong
        if (song == null) {
            miniBinding.root.visibility = View.GONE
            return
        }

        miniBinding.root.visibility = View.VISIBLE
        miniBinding.tvSongTitle.text = song.title

        Glide.with(activity)
            .load(song.getAlbumArtUri())
            .placeholder(R.drawable.ic_album_art)
            .into(miniBinding.ivAlbumArt)

        updatePlayPauseIcon()

        miniBinding.ivPlayPause.setOnClickListener {
            if (PlayerManager.isPlaying()) {
                PlayerManager.pause()
            } else {
                PlayerManager.play()
            }
            updatePlayPauseIcon()
        }

        miniBinding.root.setOnClickListener {
            val intent = Intent(activity, MusicPlayerActivity::class.java).apply {
                putExtra("SONG_LIST", ArrayList(PlayerManager.songList))
                putExtra("SELECTED_INDEX", PlayerManager.currentIndex)
            }
            activity.startActivity(intent)
        }
    }

    fun refresh(activity: Activity) {
        binding?.let {
            bindMiniPlayer(activity, it)
        }
    }

    private fun updatePlayPauseIcon() {
        binding?.ivPlayPause?.setImageResource(
            if (PlayerManager.isPlaying()) R.drawable.ic_pause_circle
            else R.drawable.ic_play_circle
        )
    }

    fun unbindMiniPlayer() {
        binding = null
    }
    fun isMiniPlayerBound(): Boolean {
        return binding != null
    }
    fun getMiniPlayerBinding(): LayoutMiniPlayerBinding? {
        return binding
    }
    fun setMiniPlayerBinding(miniBinding: LayoutMiniPlayerBinding) {
        binding = miniBinding
    }
    fun clearMiniPlayerBinding() {
        binding = null
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
