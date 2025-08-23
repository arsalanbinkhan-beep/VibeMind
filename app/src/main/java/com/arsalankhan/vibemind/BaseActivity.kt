package com.arsalankhan.vibemind

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat // Import for WindowCompat
import com.arsalankhan.vibemind.databinding.LayoutMiniPlayerBinding

open class BaseActivity : AppCompatActivity() {
    private var _miniPlayerBinding: LayoutMiniPlayerBinding? = null
    protected open val miniPlayerBinding get() = _miniPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set up song change listener - just save the last played song
        PlayerManager.onSongChanged = { song ->
            LastPlayedManager.saveLastPlayedSong(this, song)
        }
    }

    fun attachMiniPlayer(binding: LayoutMiniPlayerBinding) {
        _miniPlayerBinding = binding
        MiniPlayerManager.bindMiniPlayer(this, binding)
    }


    override fun onResume() {
        super.onResume()
        miniPlayerBinding?.let {
            MiniPlayerManager.refresh(this)
        }
        // Update notification when activity resumes
        if (PlayerManager.currentSong != null) {
            NotificationManager.updateNotification(this)
        }
        if (!isFinishing && !isDestroyed) {
            miniPlayerBinding?.let {
                MiniPlayerManager.refresh(this)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear reference to avoid leaks
        _miniPlayerBinding = null
    }
}