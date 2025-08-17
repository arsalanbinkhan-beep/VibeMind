package com.arsalankhan.vibemind

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import com.arsalankhan.vibemind.databinding.PlaylistAlbumBinding

class PlaylistAlbumActivity : BaseActivity() {

    private lateinit var binding: PlaylistAlbumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PlaylistAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Attach the mini player from the layout
        attachMiniPlayer(binding.miniPlayerLayout)

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_from_right, R.anim.slide_to_left)
            startActivity(intent, options.toBundle())
        }
    }
}
