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
        // 💡 NEW: This tells the system to draw the app content behind the system bars (like the navigation bar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
    }

    override fun onDestroy() {
        super.onDestroy()
        _miniPlayerBinding = null
    }
}