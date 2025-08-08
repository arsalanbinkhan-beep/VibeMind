package com.arsalankhan.vibemind

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arsalankhan.vibemind.databinding.LayoutMiniPlayerBinding

open class BaseActivity : AppCompatActivity() {
    private var _miniPlayerBinding: LayoutMiniPlayerBinding? = null
    protected open val miniPlayerBinding get() = _miniPlayerBinding

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
